package chemos.chem_os.dto;

import java.math.BigDecimal;

public record RevenueCsvEntryResponse(
        Long id,
        String particular,
        BigDecimal amount,
        String createdBy
) {}
