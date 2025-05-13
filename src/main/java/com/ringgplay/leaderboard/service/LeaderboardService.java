package com.ringgplay.leaderboard.service;

import com.ringgplay.leaderboard.entity.*;
import com.ringgplay.leaderboard.service.ScoreRepository;
import com.ringgplay.leaderboard.wal.WALManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
@Scope("prototype")
public class LeaderboardService {

    private final Map<String, Map<WindowType, GameLeaderboard>> allBoards = new ConcurrentHashMap<>();
    private final WALManager walManager;
    private final ScoreRepository repository;

    private final BlockingQueue<ScoreEntity> dbWriteQueue = new LinkedBlockingQueue<>(100_000); // adjustable
    private final ExecutorService dbWorker = Executors.newSingleThreadExecutor();

    @Autowired
    public LeaderboardService(WALManager walManager, ScoreRepository repository) {
        this.walManager = walManager;
        this.repository = repository;

        // Replay WAL
        walManager.replay(this);

        // Launch DB background writer
        startDbWriter();
    }

    public void ingest(String gameId, ScoreEntry entry) {
        walManager.append(gameId, entry);

        allBoards.computeIfAbsent(gameId, id -> new EnumMap<>(WindowType.class));
        for (WindowType wt : WindowType.values()) {
            allBoards.get(gameId).computeIfAbsent(wt, w -> new GameLeaderboard(100, w.getDuration()))
                    .ingestScore(entry.getUserId(), entry.getScore(), entry.getTimestamp());
        }

        // Add to async DB write queue (non-blocking)
        boolean offered = dbWriteQueue.offer(new ScoreEntity(entry, gameId));
        if (!offered) {
            log.warn("DB write queue is full. Dropping score for user {} in game {}", entry.getUserId(), gameId);
        }
    }

    public List<ScoreEntry> getTopK(String gameId, int k, WindowType window) {
        GameLeaderboard board = getBoard(gameId, window);
        return (board == null) ? List.of() : board.getTopK().stream().limit(k).toList();
    }

    public RankResponse getRank(String gameId, String userId, WindowType window) {
        GameLeaderboard board = getBoard(gameId, window);
        if (board == null) return new RankResponse(-1, -1);
        return new RankResponse(board.getRank(userId), board.getPercentile(userId));
    }

    private GameLeaderboard getBoard(String gameId, WindowType window) {
        return allBoards.getOrDefault(gameId, Map.of()).get(window);
    }

    private void startDbWriter() {
        dbWorker.submit(() -> {
            log.info("Async DB writer started...");
            List<ScoreEntity> batch = new ArrayList<>();
            while (!Thread.currentThread().isInterrupted()) {
                try {
//                    if (System.currentTimeMillis() % 5000 < 50) {  // every 5 seconds
//                        log.info("DB Queue Size: {}", dbWriteQueue.size());
//                    }
                    ScoreEntity first = dbWriteQueue.poll(2, TimeUnit.SECONDS);
                    if (first != null) {
                        batch.clear();
                        batch.add(first);
                        dbWriteQueue.drainTo(batch, 99); // up to 100 records per batch
                        repository.saveAll(batch);
                    }
                } catch (Exception e) {
                    log.error("Failed to flush score batch to DB", e);
                }
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down LeaderboardService...");
        dbWorker.shutdown();
        try {
            if (!dbWorker.awaitTermination(5, TimeUnit.SECONDS)) {
                dbWorker.shutdownNow();
            }
        } catch (InterruptedException e) {
            dbWorker.shutdownNow();
        }
    }
}