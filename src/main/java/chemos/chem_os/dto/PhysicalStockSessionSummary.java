package chemos.chem_os.dto;

import java.time.LocalDateTime;

public record PhysicalStockSessionSummary(
        String updatedBy,
        LocalDateTime sessionTimestamp,
        long entriesUpdated
) {}