package chemos.chem_os.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdatePurchaseRequest(
        String companyTo,
        String purchaseType,
        String companyFrom,
        String product,
        @JsonProperty("ship_id")
        @JsonAlias({"shipId", "vessel_id"})
        Long shipId,
        @JsonProperty("ship_name")
        @JsonAlias({"shipName", "vesselName", "vessel_name"})
        String shipName,
        String shipment,
        Double quantity,
        BigDecimal priceFc,
        String currency,
        BigDecimal offerUsd,
        BigDecimal exchangeRate,
        BigDecimal priceInr,
        String deliveryTerm,
        Integer paymentDays,
        String port,
        BigDecimal marketPrice,
        String marketStatus,
        BigDecimal replacementCost,
        String make,
        String packaging,
        String origin,
        BigDecimal expense,
        BigDecimal customDuty,
        BigDecimal sws,
        BigDecimal add,
        BigDecimal otherExpense,
        @JsonAlias("dischargePort") String dischargePorts,
        String priceType,
        Integer paymentTerm,
        LocalDate etd,
        LocalDate eta
) {}