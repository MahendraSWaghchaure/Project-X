package com.projectx.tracking.dto;

import jakarta.validation.constraints.Size;

public record TriggerSosRequest(@Size(max = 500) String note) {
}
