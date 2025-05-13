package com.ringgplay.leaderboard.service;

import com.ringgplay.leaderboard.entity.ScoreEntry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ScoreKafkaProducer {
    private final KafkaTemplate<String, ScoreEntry> template;

    public ScoreKafkaProducer(KafkaTemplate<String, ScoreEntry> template) {
        this.template = template;
    }

    public void send(String gameId, ScoreEntry entry) {
        template.send("scores", gameId, entry);
    }
}
