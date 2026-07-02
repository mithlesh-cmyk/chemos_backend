package chemos.chem_os.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PurchaseComparisonRequest(
        @JsonProperty("purchase_ids")
        List<String> purchaseIds
) {
}







