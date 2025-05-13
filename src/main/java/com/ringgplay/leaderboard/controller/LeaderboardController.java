package com.ringgplay.leaderboard.controller;

import com.ringgplay.leaderboard.entity.RankResponse;
import com.ringgplay.leaderboard.entity.ScoreEntry;
import com.ringgplay.leaderboard.entity.WindowType;
import com.ringgplay.leaderboard.service.LeaderboardService;
import com.ringgplay.leaderboard.service.ScoreKafkaProducer;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/games/{gameId}")
public class LeaderboardController {

    private final LeaderboardService service;
    private final ScoreKafkaProducer kafkaProducer;
    //private static final org.slf4j.Logger log = LoggerFactory.getLogger(LeaderboardService.class);

    public LeaderboardController(LeaderboardService service, ScoreKafkaProducer kafkaProducer) {
        this.service = service;
        this.kafkaProducer = kafkaProducer;
    }

    @PostMapping("/scores")
    public ResponseEntity<String> submitScore(@PathVariable String gameId,
                                              @RequestBody ScoreEntry entry,
                                              @RequestParam(defaultValue = "sync") String mode) {
        //long start = System.currentTimeMillis();

        if (entry.getTimestamp() == 0)
            entry.setTimestamp(System.currentTimeMillis());

        if (mode.equalsIgnoreCase("async")) {
            kafkaProducer.send(gameId, entry);
        } else {
            service.ingest(gameId,entry);
        }

        //long duration = System.currentTimeMillis() - start;
        //log.info("Processed score for user {} in {} ms (mode={})", entry.getUserId(), duration, mode);
        return ResponseEntity.ok("Score received");
    }

    @GetMapping("/leaders")
    public List<ScoreEntry> getTopK(@PathVariable String gameId,
                                    @RequestParam(defaultValue = "10") int limit,
                                    @RequestParam(defaultValue = "24h") String window) {
        return service.getTopK(gameId, limit, WindowType.fromString(window));
    }

    @GetMapping("/users/{userId}/rank")
    public RankResponse getRank(@PathVariable String gameId,
                                @PathVariable String userId,
                                @RequestParam(defaultValue = "24h") String window) {
        return service.getRank(gameId, userId, WindowType.fromString(window));
    }
}