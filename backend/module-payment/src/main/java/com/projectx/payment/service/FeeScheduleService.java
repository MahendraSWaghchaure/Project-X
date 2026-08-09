package com.projectx.payment.service;

import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.payment.domain.FeeSchedule;
import com.projectx.payment.dto.FeeScheduleResponse;
import com.projectx.payment.dto.UpsertFeeScheduleRequest;
import com.projectx.payment.repo.FeeScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FeeScheduleService {

    private final FeeScheduleRepository feeScheduleRepository;

    public FeeScheduleService(FeeScheduleRepository feeScheduleRepository) {
        this.feeScheduleRepository = feeScheduleRepository;
    }

    public FeeScheduleResponse upsert(Long schoolId, UpsertFeeScheduleRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        FeeSchedule schedule = feeScheduleRepository.findBySchoolId(schoolId)
                .orElseGet(() -> new FeeSchedule(schoolId, request.monthlyAmount()));
        schedule.setMonthlyAmount(request.monthlyAmount());
        return FeeScheduleResponse.from(feeScheduleRepository.save(schedule));
    }

    @Transactional(readOnly = true)
    public FeeScheduleResponse get(Long schoolId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return feeScheduleRepository.findBySchoolId(schoolId)
                .map(FeeScheduleResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No fee schedule configured for this school yet"));
    }
}
