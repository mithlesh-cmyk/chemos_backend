package chemos.chem_os.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseComparisonItem(
        String id,

        @JsonProperty("company_from")
        String companyFrom,

        Double quantity,

        @JsonProperty("delivery_term")
        String deliveryTerm,

        @JsonProperty("price_fc")
        BigDecimal priceFc,

        String currency,

        @JsonProperty("exchange_rate")
        BigDecimal exchangeRate,

        @JsonProperty("price_inr_per_mt")
        BigDecimal priceInrPerMt,

        @JsonProperty("valid_till")
        LocalDate validTill,

        BigDecimal expense,

        @JsonProperty("custom_duty")
        BigDecimal customDuty,

        BigDecimal sws,

        BigDecimal add,

        @JsonProperty("other_expense")
        BigDecimal otherExpense,

        @JsonProperty("landed_cost_per_mt")
        BigDecimal landedCostPerMt,

        @JsonProperty("transit_days")
        Integer transitDays
) {
}
