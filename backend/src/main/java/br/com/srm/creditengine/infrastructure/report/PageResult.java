package br.com.srm.creditengine.infrastructure.report;

import java.util.List;

public record PageResult<T>(List<T> content, long totalElements, int page, int size) {

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }
}
