package com.projectx.hardware.dto;

/** stored=false means this exact (deviceId, timestamp) was already recorded - a safe retry, not an error. */
public record IngestResultResponse(boolean stored) {
}
