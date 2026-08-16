package com.projectx.tracking.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "projectx.tracking.safety")
public class TrackingSafetyProperties {

    /** A school van/bus reported faster than this is flagged as overspeeding. */
    private double speedLimitKmh = 60.0;

    /** A drop in reported speed of at least this much between consecutive pings within brakingWindowSeconds is flagged as harsh braking. */
    private double harshBrakingDeltaKmh = 25.0;

    private int brakingWindowSeconds = 15;

    /** Minimum gap between two flags of the same type on the same trip, so a sustained incident is one flag, not one per ping. */
    private int cooldownSeconds = 300;

    public double getSpeedLimitKmh() {
        return speedLimitKmh;
    }

    public void setSpeedLimitKmh(double speedLimitKmh) {
        this.speedLimitKmh = speedLimitKmh;
    }

    public double getHarshBrakingDeltaKmh() {
        return harshBrakingDeltaKmh;
    }

    public void setHarshBrakingDeltaKmh(double harshBrakingDeltaKmh) {
        this.harshBrakingDeltaKmh = harshBrakingDeltaKmh;
    }

    public int getBrakingWindowSeconds() {
        return brakingWindowSeconds;
    }

    public void setBrakingWindowSeconds(int brakingWindowSeconds) {
        this.brakingWindowSeconds = brakingWindowSeconds;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }
}
