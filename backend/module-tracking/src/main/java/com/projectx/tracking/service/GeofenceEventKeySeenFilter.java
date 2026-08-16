package com.projectx.tracking.service;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import java.nio.charset.StandardCharsets;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * A cheap, in-memory negative-cache in front of GeofenceEventRepository's exists-then-insert
 * dedup check, for high ping volume. Bloom filters can false-positive ("might have seen this
 * before" when it's actually new) but never false-negative - so this is only ever used to SKIP
 * the redundant existence check when we're certain a key is brand new (never seen means never
 * seen), not to skip raising a real event. Every "maybe seen" key still falls through to the real
 * DB check, which stays the single source of truth. Sized for ~500K distinct trip/type/child keys
 * a day at 1% false-positive rate; reset daily since geofence events are trip-scoped and trips are
 * per-date, so nothing from yesterday needs to stay in the set.
 */
@Component
public class GeofenceEventKeySeenFilter {

    private volatile BloomFilter<CharSequence> filter = newFilter();

    private static BloomFilter<CharSequence> newFilter() {
        return BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 500_000, 0.01);
    }

    /** Records this key as seen; returns true only if it was DEFINITELY not seen before this call. */
    public boolean definitelyNotSeenBefore(String key) {
        boolean mightHaveBeenSeen = filter.mightContain(key);
        filter.put(key);
        return !mightHaveBeenSeen;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Kolkata")
    public void resetDaily() {
        filter = newFilter();
    }
}
