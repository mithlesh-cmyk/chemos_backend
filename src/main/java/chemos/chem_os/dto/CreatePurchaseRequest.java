package chemos.chem_os.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePurchaseRequest(

        @JsonProperty("company_to")
        @NotBlank(message = "companyTo is required")
        String companyTo,

        @JsonProperty("purchase_type")
        String purchaseType,

        @JsonProperty("company_from")
        @NotBlank(message = "companyFrom is required")
        String companyFrom,

        @NotBlank(message = "product is required")
        String product,

        @JsonProperty("vessel_name")
        String vesselName,

        String shipment,

        @NotNull(message = "quantity is required")
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

        @NotBlank(message = "port is required")
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

        @NotBlank(message = "origin is required")
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
