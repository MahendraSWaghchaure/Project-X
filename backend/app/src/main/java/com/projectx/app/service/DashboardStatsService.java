package com.projectx.app.service;

import com.projectx.app.dto.PlatformDashboardStatsResponse;
import com.projectx.app.dto.SchoolDashboardStatsResponse;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.grievance.domain.GrievanceStatus;
import com.projectx.grievance.repo.GrievanceRepository;
import com.projectx.payment.domain.PayrollStatus;
import com.projectx.payment.domain.PaymentStatus;
import com.projectx.payment.domain.WaiverStatus;
import com.projectx.payment.repo.LateFeeWaiverRequestRepository;
import com.projectx.payment.repo.PayrollRecordRepository;
import com.projectx.payment.repo.PaymentRepository;
import com.projectx.people.domain.DriverProfile;
import com.projectx.people.domain.DriverVerificationStatus;
import com.projectx.people.repo.ChildRepository;
import com.projectx.people.repo.DriverProfileRepository;
import com.projectx.school.domain.SchoolStatus;
import com.projectx.school.domain.VehicleStatus;
import com.projectx.school.repo.RouteRepository;
import com.projectx.school.repo.SchoolRepository;
import com.projectx.school.repo.VehicleRepository;
import com.projectx.tracking.domain.TripStatus;
import com.projectx.tracking.repo.TripRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Powers both admin dashboards' stat cards with indexed COUNT/SUM queries instead of the
 * frontend fetching every row of every underlying table just to compute a length or a sum -
 * the pattern that broke down as soon as a school (or the platform) had more than a page of
 * data. Lives in {@code app} rather than any single feature module because it's the only module
 * allowed to depend on school + people + tracking + grievance + payment simultaneously - see
 * ProjectXApplication's javadoc on why app is the cross-module composition root.
 *
 * <p>The frontend polls these on an interval (see the dashboard pages) rather than this service
 * pushing updates - a handful of cheap indexed aggregate queries every 15-20s scales fine on the
 * existing single-VM deployment without standing up a new push channel for what is, after all,
 * just a number on a card.
 */
@Service
@Transactional(readOnly = true)
public class DashboardStatsService {

    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");
    private static final List<GrievanceStatus> OPEN_GRIEVANCE_STATUSES = List.of(GrievanceStatus.OPEN, GrievanceStatus.IN_PROGRESS);
    private static final List<TripStatus> ACTIVE_TRIP_STATUSES = List.of(TripStatus.IN_PROGRESS, TripStatus.DELAYED);
    private static final List<PayrollStatus> PENDING_PAYROLL_STATUSES = List.of(PayrollStatus.PENDING, PayrollStatus.PROCESSING);

    private final SchoolRepository schoolRepository;
    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final ChildRepository childRepository;
    private final TripRepository tripRepository;
    private final GrievanceRepository grievanceRepository;
    private final PaymentRepository paymentRepository;
    private final PayrollRecordRepository payrollRecordRepository;
    private final LateFeeWaiverRequestRepository waiverRequestRepository;

    public DashboardStatsService(SchoolRepository schoolRepository, VehicleRepository vehicleRepository,
            RouteRepository routeRepository, DriverProfileRepository driverProfileRepository,
            ChildRepository childRepository, TripRepository tripRepository, GrievanceRepository grievanceRepository,
            PaymentRepository paymentRepository, PayrollRecordRepository payrollRecordRepository,
            LateFeeWaiverRequestRepository waiverRequestRepository) {
        this.schoolRepository = schoolRepository;
        this.vehicleRepository = vehicleRepository;
        this.routeRepository = routeRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.childRepository = childRepository;
        this.tripRepository = tripRepository;
        this.grievanceRepository = grievanceRepository;
        this.paymentRepository = paymentRepository;
        this.payrollRecordRepository = payrollRecordRepository;
        this.waiverRequestRepository = waiverRequestRepository;
    }

    public PlatformDashboardStatsResponse platformStats() {
        return new PlatformDashboardStatsResponse(
                schoolRepository.count(),
                schoolRepository.countByStatus(SchoolStatus.ACTIVE),
                schoolRepository.countByStatus(SchoolStatus.SUSPENDED),
                driverProfileRepository.countBySchoolIdIsNull(),
                grievanceRepository.countByStatusIn(OPEN_GRIEVANCE_STATUSES),
                waiverRequestRepository.countByStatus(WaiverStatus.PENDING));
    }

    public SchoolDashboardStatsResponse schoolStats(Long schoolId) {
        CurrentUserContext.requireSchoolAccess(schoolId);

        LocalDate today = LocalDate.now(INDIA_ZONE);
        YearMonth thisMonth = YearMonth.from(today);
        Instant monthStart = thisMonth.atDay(1).atStartOfDay(INDIA_ZONE).toInstant();
        Instant monthEndExclusive = thisMonth.plusMonths(1).atDay(1).atStartOfDay(INDIA_ZONE).toInstant();

        // PayrollRecord has no schoolId of its own (see PayrollRecordRepository) - resolve the
        // school's driver ids first, a set bounded by one school's headcount, not the whole platform.
        List<Long> driverIds = driverProfileRepository.findBySchoolId(schoolId).stream().map(DriverProfile::getId).toList();
        long payrollPending = driverIds.isEmpty() ? 0
                : payrollRecordRepository.countByDriverIdInAndStatusIn(driverIds, PENDING_PAYROLL_STATUSES);
        long payrollFailed = driverIds.isEmpty() ? 0
                : payrollRecordRepository.countByDriverIdInAndStatusIn(driverIds, List.of(PayrollStatus.FAILED));

        BigDecimal revenueThisMonth = paymentRepository.sumAmountForSchoolByStatusBetween(
                schoolId, PaymentStatus.SUCCESS, monthStart, monthEndExclusive);

        return new SchoolDashboardStatsResponse(
                vehicleRepository.countBySchoolId(schoolId),
                vehicleRepository.countBySchoolIdAndStatus(schoolId, VehicleStatus.ACTIVE),
                driverProfileRepository.countBySchoolId(schoolId),
                driverProfileRepository.countBySchoolIdAndVerificationStatus(schoolId, DriverVerificationStatus.VERIFIED),
                driverProfileRepository.countBySchoolIdIsNull(),
                routeRepository.countBySchoolId(schoolId),
                childRepository.countBySchoolId(schoolId),
                tripRepository.countBySchoolIdAndTripDate(schoolId, today),
                tripRepository.countBySchoolIdAndTripDateAndStatusIn(schoolId, today, ACTIVE_TRIP_STATUSES),
                tripRepository.countBySchoolIdAndTripDateAndStatus(schoolId, today, TripStatus.DELAYED),
                grievanceRepository.countBySchoolIdAndStatusIn(schoolId, OPEN_GRIEVANCE_STATUSES),
                revenueThisMonth,
                payrollPending,
                payrollFailed);
    }
}
