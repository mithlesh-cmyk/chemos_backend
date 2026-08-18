package chemos.chem_os.dto;

import java.math.BigDecimal;

public record ProductPortFinancialSummaryResponse(
        String product,
        String port,
        Double physicalStock,
        Double physicalUnsold,
        Double soldUnlifted,
        Double quantityReceived,
        String companyName,
        BigDecimal averageWeightedCost,
        BigDecimal averageWeightedSale
) {
}
