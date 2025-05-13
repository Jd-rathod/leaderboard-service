package com.ringgplay.leaderboard.service;

import com.ringgplay.leaderboard.entity.ScoreEntry;

import java.time.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GameLeaderboard {
    private final TreeMap<Integer, Set<String>> scoreToUsers = new TreeMap<>(Collections.reverseOrder());
    private final Map<String, ScoreEntry> userScores = new HashMap<>();
    private final PriorityQueue<ScoreEntry> topKHeap;
    private final Deque<ScoreEntry> slidingWindow = new ArrayDeque<>();
    private final int topKLimit;
    private final Duration windowSize;

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public GameLeaderboard(int topKLimit, Duration windowSize) {
        this.topKLimit = topKLimit;
        this.windowSize = windowSize;
        this.topKHeap = new PriorityQueue<>(Comparator.comparingInt(ScoreEntry::getScore));
    }

    public void ingestScore(String userId, int score, long timestamp) {
        rwLock.writeLock().lock();
        try {
            ScoreEntry entry = new ScoreEntry(userId, score, timestamp);
            ScoreEntry prev = userScores.get(userId);

            if (prev == null || score > prev.getScore()) {
                userScores.put(userId, entry);
                slidingWindow.addLast(entry);

                scoreToUsers.computeIfAbsent(score, k -> new HashSet<>()).add(userId);
                if (prev != null) {
                    Set<String> users = scoreToUsers.get(prev.getScore());
                    users.remove(userId);
                    if (users.isEmpty()) scoreToUsers.remove(prev.getScore());
                }

                if (topKHeap.size() < topKLimit) {
                    topKHeap.offer(entry);
                } else if (topKHeap.peek() != null && topKHeap.peek().getScore() < score) {
                    topKHeap.poll();
                    topKHeap.offer(entry);
                }
            }

            evictOldScores();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void evictOldScores() {
        long cutoff = Instant.now().minus(windowSize).toEpochMilli();
        while (!slidingWindow.isEmpty() && slidingWindow.peekFirst().getTimestamp() < cutoff) {
            ScoreEntry old = slidingWindow.pollFirst();
            if (userScores.get(old.getUserId()).getTimestamp() == old.getTimestamp()) {
                userScores.remove(old.getUserId());
                Set<String> users = scoreToUsers.get(old.getScore());
                users.remove(old.getUserId());
                if (users.isEmpty()) scoreToUsers.remove(old.getScore());
            }
        }
    }

    public List<ScoreEntry> getTopK() {
        rwLock.readLock().lock();
        try {
            List<ScoreEntry> result = new ArrayList<>(topKHeap);
            result.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
            return result;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public int getRank(String userId) {
        rwLock.readLock().lock();
        try {
            ScoreEntry entry = userScores.get(userId);
            if (entry == null) return -1;

            int rank = 1;
            for (Map.Entry<Integer, Set<String>> e : scoreToUsers.entrySet()) {
                if (e.getKey() > entry.getScore()) {
                    rank += e.getValue().size();
                } else break;
            }
            return rank;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public double getPercentile(String userId) {
        rwLock.readLock().lock();
        try {
            int rank = getRank(userId);
            if (rank == -1) return -1;
            int total = userScores.size();
            return 100.0 * (1 - ((rank - 1.0) / total));
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
