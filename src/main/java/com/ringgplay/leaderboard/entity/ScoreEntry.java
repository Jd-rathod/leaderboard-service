package com.ringgplay.leaderboard.entity;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScoreEntry {
    @Getter @Setter
    private String userId;
    private int score;
    private long timestamp;
}