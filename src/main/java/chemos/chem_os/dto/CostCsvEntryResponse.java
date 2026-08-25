package chemos.chem_os.dto;

import java.math.BigDecimal;

public record CostCsvEntryResponse(
        Long id,
        String particular,
        BigDecimal directCost,
        BigDecimal indirectCost,
        String createdBy
) {}
