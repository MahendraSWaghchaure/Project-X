package com.projectx.grievance.service;

import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.security.Role;
import com.projectx.grievance.domain.Grievance;
import com.projectx.grievance.dto.CreateGrievanceRequest;
import com.projectx.grievance.dto.GrievanceResponse;
import com.projectx.grievance.dto.UpdateGrievanceStatusRequest;
import com.projectx.grievance.repo.GrievanceRepository;
import com.projectx.grievance.ws.GrievanceBroadcaster;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GrievanceService {

    private final GrievanceRepository grievanceRepository;
    private final GrievanceBroadcaster broadcaster;

    public GrievanceService(GrievanceRepository grievanceRepository, GrievanceBroadcaster broadcaster) {
        this.grievanceRepository = grievanceRepository;
        this.broadcaster = broadcaster;
    }

    /** Any of the three roles that live a trip day-to-day (parent, driver, school admin) can raise one. */
    public GrievanceResponse raise(CreateGrievanceRequest request) {
        CurrentUser user = CurrentUserContext.get();
        if (user.role() == Role.PLATFORM_ADMIN) {
            throw new ForbiddenOperationException("Platform admin resolves grievances, not raise them");
        }
        Long schoolId = request.schoolId() != null ? request.schoolId() : user.schoolId();
        // SCHOOL_ADMIN/DRIVER tokens carry a schoolId claim and must raise about their own school;
        // PARENT tokens never carry one (guardianship-scoped, not school-scoped - see CurrentUser's
        // javadoc), so there's nothing to check them against here and their claimed schoolId is
        // trusted at face value, same as CreateChildRequest.primaryParentId elsewhere.
        if (schoolId != null && user.role() != Role.PARENT) {
            CurrentUserContext.requireSchoolAccess(schoolId);
        }

        Grievance grievance = new Grievance(schoolId, user.userId(), user.role(), user.email(),
                request.category(), request.subject(), request.description());
        GrievanceResponse response = GrievanceResponse.from(grievanceRepository.save(grievance));
        broadcaster.broadcast(response);
        return response;
    }

    @Transactional(readOnly = true)
    public List<GrievanceResponse> listMine() {
        Long userId = CurrentUserContext.get().userId();
        return grievanceRepository.findByRaisedByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(GrievanceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<GrievanceResponse> listForSchool(Long schoolId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return grievanceRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId).stream()
                .map(GrievanceResponse::from).toList();
    }

    /** Platform-wide view - across every school, including grievances raised with no school context. */
    @Transactional(readOnly = true)
    public List<GrievanceResponse> listAll() {
        return grievanceRepository.findAllByOrderByCreatedAtDesc().stream().map(GrievanceResponse::from).toList();
    }

    public GrievanceResponse updateStatus(Long grievanceId, UpdateGrievanceStatusRequest request) {
        CurrentUser user = CurrentUserContext.get();
        Grievance grievance = grievanceRepository.findById(grievanceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Grievance", grievanceId));
        if (!user.belongsToSchool(grievance.getSchoolId())) {
            throw new ForbiddenOperationException("Not authorized to resolve this grievance");
        }

        grievance.resolve(request.status(), request.resolutionNote(), user.userId());
        GrievanceResponse response = GrievanceResponse.from(grievance);
        broadcaster.broadcast(response);
        return response;
    }
}
