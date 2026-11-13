package com.projectx.attendance.service;

/** Module-internal: a roster student was linked to a transport child (published inside the link transaction, consumed AFTER_COMMIT). */
public record StudentChildLinkedEvent(Long studentId, Long childId) {
}
