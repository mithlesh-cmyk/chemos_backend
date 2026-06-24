package chemos.chem_os.dto;

import java.util.List;
import java.util.Map;

public record PurchaseComparisonResponse(
        List<PurchaseComparisonItem> purchases,
        Map<String, FieldHighlight> highlights
) {
}
