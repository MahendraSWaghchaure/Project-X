package com.projectx.tracking.service;

import com.projectx.people.domain.Child;
import com.projectx.people.repo.ChildRepository;
import com.projectx.tracking.domain.Trip;
import com.projectx.tracking.dto.TripManifestEntry;
import com.projectx.tracking.repo.ChildAbsenceRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** What the driver actually sees for a trip - the route's children, each flagged if their parent marked them absent for the trip's date. */
@Service
@Transactional(readOnly = true)
public class TripManifestService {

    private final TripService tripService;
    private final ChildRepository childRepository;
    private final ChildAbsenceRepository childAbsenceRepository;

    public TripManifestService(TripService tripService, ChildRepository childRepository,
                                ChildAbsenceRepository childAbsenceRepository) {
        this.tripService = tripService;
        this.childRepository = childRepository;
        this.childAbsenceRepository = childAbsenceRepository;
    }

    public List<TripManifestEntry> getManifest(Long tripId) {
        Trip trip = tripService.findAndAuthorizeAsDriverOrSchoolAdmin(tripId);
        return childRepository.findByRouteId(trip.getRouteId()).stream()
                .map(child -> toEntry(child, trip))
                .toList();
    }

    private TripManifestEntry toEntry(Child child, Trip trip) {
        boolean absent = childAbsenceRepository.existsByChildIdAndAbsenceDate(child.getId(), trip.getTripDate());
        return new TripManifestEntry(child.getId(), child.getFirstName(), child.getLastName(), absent);
    }
}
