package com.switchtx.infrastructure.adapter.in.rest.common;

import com.switchtx.application.common.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.function.Function;

@Schema(description = "Página de resultados")
public record PageResponse<T>(
        @Schema(description = "Elementos de la página") List<T> content,
        @Schema(description = "Número de página (base 0)", example = "0") int page,
        @Schema(description = "Tamaño de página", example = "20") int size,
        @Schema(description = "Total de elementos", example = "42") long totalElements,
        @Schema(description = "Total de páginas", example = "3") int totalPages) {

    public static <D, R> PageResponse<R> from(PageResult<D> result, Function<D, R> mapper) {
        PageResult<R> mapped = result.map(mapper);
        return new PageResponse<>(mapped.content(), mapped.page(), mapped.size(), mapped.totalElements(),
                mapped.totalPages());
    }
}
