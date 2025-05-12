package com.bohuai.ocs.common;

import java.util.List;

public record PageResult<T>(List<T> items, int page, int size, long total, int totalPages) {

    public static <T> PageResult<T> of(List<T> items, int page, int size, long total) {
        int pages = size == 0 ? 0 : (int) ((total + size - 1) / size);
        return new PageResult<>(items, page, size, total, pages);
    }
}
