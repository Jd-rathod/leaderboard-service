package com.ringgplay.leaderboard.entity;

import com.ringgplay.leaderboard.service.LeaderboardService;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

public class ShardRouter {
    private final int totalShards;
    private final LeaderboardService[] shardServices;

    public ShardRouter(int totalShards, Supplier<LeaderboardService> serviceFactory) {
        this.totalShards = totalShards;
        this.shardServices = new LeaderboardService[totalShards];
        for (int i = 0; i < totalShards; i++) {
            this.shardServices[i] = serviceFactory.get();
        }
    }

    private int getShardIndex(String gameId) {
        return Math.abs(gameId.hashCode()) % totalShards;
    }

    public LeaderboardService route(String gameId) {
        return shardServices[getShardIndex(gameId)];
    }
}
