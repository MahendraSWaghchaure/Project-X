package com.projectx.payment.dto;

import com.projectx.payment.domain.FeeSchedule;
import java.math.BigDecimal;

public record FeeScheduleResponse(Long id, Long schoolId, BigDecimal monthlyAmount) {

    public static FeeScheduleResponse from(FeeSchedule schedule) {
        return new FeeScheduleResponse(schedule.getId(), schedule.getSchoolId(), schedule.getMonthlyAmount());
    }
}
