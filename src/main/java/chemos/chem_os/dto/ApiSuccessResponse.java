package chemos.chem_os.dto;

import lombok.Builder;

@Builder
public record ApiSuccessResponse<T>(
        String message,
        T data
) {
}