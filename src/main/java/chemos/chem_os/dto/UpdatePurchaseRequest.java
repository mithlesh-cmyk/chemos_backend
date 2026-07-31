package chemos.chem_os.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdatePurchaseRequest(
        String companyTo,
        String purchaseType,
        String companyFrom,
        String product,
        String vesselName,
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
        String dischargePorts,
        String priceType,
        Integer paymentTerm,
        LocalDate etd,
        LocalDate eta
) {}