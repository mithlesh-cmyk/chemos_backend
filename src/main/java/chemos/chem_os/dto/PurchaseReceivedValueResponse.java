package chemos.chem_os.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record PurchaseReceivedValueResponse(
        @JsonProperty("total_value")
        BigDecimal totalValue,

        @JsonProperty("purchase_count")
        long purchaseCount
) {
}
