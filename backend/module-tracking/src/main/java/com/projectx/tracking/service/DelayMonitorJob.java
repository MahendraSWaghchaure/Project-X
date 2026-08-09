package com.projectx.tracking.service;

import com.projectx.school.domain.School;
import com.projectx.school.repo.SchoolRepository;
import com.projectx.tracking.domain.Trip;
import com.projectx.tracking.domain.TripDirection;
import com.projectx.tracking.domain.TripStatus;
import com.projectx.tracking.repo.TripRepository;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Excessive delay after end of school day" from the product spec. India-only product for now,
 * so the dismissal-time comparison is hardcoded to IST rather than per-school timezone config -
 * revisit if this ever expands beyond India.
 */
@Component
public class DelayMonitorJob {

    private static final Logger log = LoggerFactory.getLogger(DelayMonitorJob.class);
    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");
    private static final Duration DELAY_THRESHOLD = Duration.ofMinutes(45);

    private final TripRepository tripRepository;
    private final SchoolRepository schoolRepository;
    private final GeofenceEvaluationService geofenceEvaluationService;

    public DelayMonitorJob(TripRepository tripRepository, SchoolRepository schoolRepository,
                            GeofenceEvaluationService geofenceEvaluationService) {
        this.tripRepository = tripRepository;
        this.schoolRepository = schoolRepository;
        this.geofenceEvaluationService = geofenceEvaluationService;
    }

    @Scheduled(fixedDelayString = "${projectx.tracking.delay-monitor.interval-ms:120000}")
    @Transactional
    public void checkForDelays() {
        LocalTime nowIndia = LocalTime.now(INDIA_ZONE);
        List<Trip> activeTrips = tripRepository.findByStatusIn(List.of(TripStatus.IN_PROGRESS, TripStatus.DELAYED));

        for (Trip trip : activeTrips) {
            if (trip.getDirection() != TripDirection.AFTERNOON_DROP) {
                continue;
            }
            School school = schoolRepository.findById(trip.getSchoolId()).orElse(null);
            if (school == null || school.getDismissalTime() == null) {
                continue;
            }
            LocalTime delayThreshold = school.getDismissalTime().plus(DELAY_THRESHOLD);
            if (nowIndia.isAfter(delayThreshold)) {
                log.info("Trip {} flagged as delayed - past {} (dismissal + {})", trip.getId(), delayThreshold, DELAY_THRESHOLD);
                trip.markDelayed();
                geofenceEvaluationService.raiseEndOfDayDelay(trip);
            }
        }
    }
}
