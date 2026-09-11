package com.projectx.common.web;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Standard shape for a paginated list response, carried as the {@code data} field of
 * {@code ApiResponse<PageResponse<T>>}. Keeps every paginated endpoint in the app returning the
 * same envelope instead of each one inventing its own page/size/total naming.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages, boolean last) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isLast());
    }
}
