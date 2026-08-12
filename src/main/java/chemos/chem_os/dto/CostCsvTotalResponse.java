package chemos.chem_os.dto;

import java.math.BigDecimal;

public record CostCsvTotalResponse(
        BigDecimal totalDirectCost,
        BigDecimal totalIndirectCost
) {}
