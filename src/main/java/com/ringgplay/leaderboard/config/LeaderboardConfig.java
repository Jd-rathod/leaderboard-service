package com.ringgplay.leaderboard.config;

import com.ringgplay.leaderboard.entity.ShardRouter;
import com.ringgplay.leaderboard.service.LeaderboardService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LeaderboardConfig {

    @Value("${leaderboard.shard-count:10}")
    private int shardCount;

    @Bean
    public ShardRouter shardRouter(ObjectProvider<LeaderboardService> serviceProvider) {
        return new ShardRouter(shardCount, serviceProvider::getObject);
    }
}