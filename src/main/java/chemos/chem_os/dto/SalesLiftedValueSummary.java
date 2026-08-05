package chemos.chem_os.dto;

import java.util.List;

public record SalesLiftedValueSummary(
        Double grandTotal,
        List<SalesLiftedValueByType> byType
) {
}
