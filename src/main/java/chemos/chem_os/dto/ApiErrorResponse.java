package chemos.chem_os.dto;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        String error,
        String message,
        LocalDateTime timestamp
) {
}
