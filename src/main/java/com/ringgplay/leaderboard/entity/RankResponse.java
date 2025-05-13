package com.ringgplay.leaderboard.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankResponse {
    private int rank;
    private double percentile;
}
