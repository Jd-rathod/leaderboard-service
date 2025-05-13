package com.ringgplay.leaderboard.service;

import com.ringgplay.leaderboard.entity.ScoreEntry;
import com.ringgplay.leaderboard.entity.ShardRouter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
public class ScoreKafkaConsumer {

    private final ShardRouter router;

    public ScoreKafkaConsumer(ShardRouter router) {
        this.router = router;
    }

    @KafkaListener(topics = "scores", groupId = "leaderboard-consumer")
    public void consume(ConsumerRecord<String, ScoreEntry> record) {
        String gameId = record.key();
        ScoreEntry entry = record.value();
        router.route(gameId).ingest(gameId, entry);
    }
}
