package chemos.chem_os.dto;

import java.time.LocalDateTime;

public record CostCsvUploadResponse(
        Long uploadId,
        String uploadedBy,
        LocalDateTime uploadedAt,
        int rowCount
) {}
