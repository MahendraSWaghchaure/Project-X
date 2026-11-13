package com.projectx.school.dto;

/** Counts behind the All | Active | Suspended tabs of the platform-admin schools list (indexed COUNTs, no row fetch). */
public record SchoolStatusCounts(long all, long active, long suspended) {
}
