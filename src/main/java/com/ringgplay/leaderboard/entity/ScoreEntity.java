package com.ringgplay.leaderboard.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scores")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScoreEntity {
    @Id
    @GeneratedValue
    private Long id;
    private String userId;
    private String gameId;
    private int score;
    private long timestamp;

    public ScoreEntity(ScoreEntry e, String gameId) {
        this.userId = e.getUserId();
        this.score = e.getScore();
        this.timestamp = e.getTimestamp();
        this.gameId = gameId;
    }
}
