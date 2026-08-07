package br.com.srm.creditengine.api.report;

import br.com.srm.creditengine.infrastructure.report.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Fatia de resultados paginada no servidor")
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    static <T> PageResponse<T> from(PageResult<T> result) {
        return new PageResponse<>(
                result.content(), result.page(), result.size(), result.totalElements(), result.totalPages());
    }
}
