package com.projectx.attendance.domain;

public enum ArrivalOutcome {
    PENDING,
    SAFE,
    ALERT;

    /**
     * The single place the verification rule lives. ALERT if the school marked the child ABSENT or
     * a parent disputed the arrival (safety-first: either signal alone is enough). SAFE needs at
     * least one positive confirmation, and a SCHOOL_DROP additionally needs the school itself to
     * confirm PRESENT - a parent tapping "approve" for a drop is not proof the child is inside the
     * building. Everything else is still PENDING.
     */
    public static ArrivalOutcome compute(ArrivalKind kind, SchoolConfirm school, ParentResponse parent) {
        if (school == SchoolConfirm.ABSENT || parent == ParentResponse.DISPUTED) {
            return ALERT;
        }
        boolean confirmed = school == SchoolConfirm.PRESENT || parent == ParentResponse.APPROVED;
        boolean schoolOkForKind = kind != ArrivalKind.SCHOOL_DROP || school == SchoolConfirm.PRESENT;
        return confirmed && schoolOkForKind ? SAFE : PENDING;
    }
}
