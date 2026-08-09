package com.projectx.common.web;

import java.util.List;

public record ApiError(String code, String message, List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {}

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of());
    }
}
