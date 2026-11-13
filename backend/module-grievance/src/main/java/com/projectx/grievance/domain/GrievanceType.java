package com.projectx.grievance.domain;

import com.projectx.common.security.Role;
import java.util.EnumSet;
import java.util.Set;

/**
 * What the complaint is about. Which types a submitter may pick depends on their role: a driver cannot file a complaint
 * about "driver behavior" (that is a parent's complaint about the driver) and a parent cannot file one about
 * "student/parent behavior" (that is a driver's complaint about the people they carry).
 */
public enum GrievanceType {
    VEHICLE_CONDITION,
    DRIVER_BEHAVIOR,
    ROUTE_DELAY,
    SAFETY_CONCERN,
    PAYMENT_ISSUE,
    STUDENT_PARENT_BEHAVIOR,
    SCHOOL_COORDINATION,
    OTHER;

    private static final Set<GrievanceType> DRIVER_TYPES = EnumSet.of(VEHICLE_CONDITION, ROUTE_DELAY, SAFETY_CONCERN,
            PAYMENT_ISSUE, STUDENT_PARENT_BEHAVIOR, SCHOOL_COORDINATION, OTHER);
    private static final Set<GrievanceType> PARENT_TYPES = EnumSet.of(VEHICLE_CONDITION, DRIVER_BEHAVIOR, ROUTE_DELAY,
            SAFETY_CONCERN, PAYMENT_ISSUE, SCHOOL_COORDINATION, OTHER);

    public static Set<GrievanceType> allowedFor(Role role) {
        return switch (role) {
            case DRIVER -> DRIVER_TYPES;
            case PARENT -> PARENT_TYPES;
            // A school admin may escalate anything they see on the ground.
            case SCHOOL_ADMIN -> EnumSet.allOf(GrievanceType.class);
            default -> EnumSet.noneOf(GrievanceType.class);
        };
    }

    /** The legacy coarse category kept on the row for old clients. */
    public GrievanceCategory legacyCategory() {
        return switch (this) {
            case VEHICLE_CONDITION -> GrievanceCategory.VEHICLE_CONDITION;
            case DRIVER_BEHAVIOR -> GrievanceCategory.DRIVER_CONDUCT;
            case ROUTE_DELAY -> GrievanceCategory.DELAY;
            case SAFETY_CONCERN -> GrievanceCategory.SAFETY;
            case PAYMENT_ISSUE -> GrievanceCategory.PAYMENT;
            default -> GrievanceCategory.OTHER;
        };
    }

    /** Best-effort type for a request that only carries the legacy category. */
    public static GrievanceType fromLegacy(GrievanceCategory category) {
        return switch (category) {
            case SAFETY -> SAFETY_CONCERN;
            case VEHICLE_CONDITION -> VEHICLE_CONDITION;
            case DRIVER_CONDUCT -> DRIVER_BEHAVIOR;
            case PAYMENT -> PAYMENT_ISSUE;
            case DELAY -> ROUTE_DELAY;
            case OTHER -> OTHER;
        };
    }
}
