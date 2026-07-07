package chemos.chem_os.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePurchaseRequest(

        @JsonProperty("company_to")
        String companyTo,

        @JsonProperty("purchase_type")
        String purchaseType,

        @JsonProperty("company_from")
        String companyFrom,

        String product,

        @JsonProperty("vessel_name")
        String vesselName,

        String shipment,

        Double quantity,

        String currency,

        @JsonProperty("offer_usd")
        BigDecimal offerUsd,

        @JsonProperty("exchange_rate")
        BigDecimal exchangeRate,

        @JsonProperty("price_inr")
        BigDecimal priceInr,

        @JsonProperty("delivery_term")
        String deliveryTerm,

        @JsonProperty("payment_days")
        Integer paymentDays,

        String port,

        @JsonProperty("market_status")
        String marketStatus,

        @JsonProperty("market_price")
        BigDecimal marketPrice,

        @JsonProperty("replacement_cost")
        BigDecimal replacementCost,

        @JsonProperty("price_fc")
        BigDecimal priceFc,

        String make,

        String packaging,

        String origin,

        BigDecimal expense,

        @JsonProperty("custom_duty")
        BigDecimal customDuty,

        BigDecimal sws,

        BigDecimal add,

        @JsonProperty("other_expense")
        BigDecimal otherExpense,

        @JsonProperty("discharge_ports")
        String dischargePorts,

        @JsonProperty("price_type")
        String priceType,

        @JsonProperty("payment_term")
        Integer paymentTerm,

        LocalDate etd,

        LocalDate eta
) {
}
