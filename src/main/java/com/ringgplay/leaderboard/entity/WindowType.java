package com.ringgplay.leaderboard.entity;

import java.time.Duration;

public enum WindowType {
    H24(Duration.ofHours(24)),
    D3(Duration.ofDays(3)),
    D7(Duration.ofDays(7));

    private final Duration duration;

    WindowType(Duration d) { this.duration = d; }
    public Duration getDuration() { return duration; }

    public static WindowType fromString(String str) {
        return switch (str.toLowerCase()) {
            case "24h" -> H24;
            case "3d" -> D3;
            case "7d" -> D7;
            default -> H24; // fallback
        };
    }
}