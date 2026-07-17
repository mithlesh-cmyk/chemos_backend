package chemos.chem_os.dto;

import java.time.LocalDateTime;

public record PlUploadResponse(
        Long uploadId,
        String uploadedBy,
        LocalDateTime uploadedAt,
        int rowCount
) {}
