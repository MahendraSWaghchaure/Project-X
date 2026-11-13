package com.projectx.attendance.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Exhaustive check of the plan's outcome rule: 3 kinds x 3 school states x 3 parent states. */
class ArrivalOutcomeTest {

    @Test
    void outcomeMatchesThePlanRuleForEveryCombination() {
        for (ArrivalKind kind : ArrivalKind.values()) {
            for (SchoolConfirm school : SchoolConfirm.values()) {
                for (ParentResponse parent : ParentResponse.values()) {
                    ArrivalOutcome expected;
                    if (school == SchoolConfirm.ABSENT || parent == ParentResponse.DISPUTED) {
                        expected = ArrivalOutcome.ALERT;
                    } else if ((kind != ArrivalKind.SCHOOL_DROP || school == SchoolConfirm.PRESENT)
                            && (school == SchoolConfirm.PRESENT || parent == ParentResponse.APPROVED)) {
                        expected = ArrivalOutcome.SAFE;
                    } else {
                        expected = ArrivalOutcome.PENDING;
                    }
                    assertThat(ArrivalOutcome.compute(kind, school, parent))
                            .as("%s / school=%s / parent=%s", kind, school, parent)
                            .isEqualTo(expected);
                }
            }
        }
    }

    @Test
    void parentApprovalAloneNeverMakesASchoolDropSafe() {
        assertThat(ArrivalOutcome.compute(ArrivalKind.SCHOOL_DROP, SchoolConfirm.PENDING, ParentResponse.APPROVED))
                .isEqualTo(ArrivalOutcome.PENDING);
        assertThat(ArrivalOutcome.compute(ArrivalKind.HOME_PICKUP, SchoolConfirm.PENDING, ParentResponse.APPROVED))
                .isEqualTo(ArrivalOutcome.SAFE);
    }
}
