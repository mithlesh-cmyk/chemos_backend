package chemos.chem_os.dto;

import java.math.BigDecimal;

public record PlEntryResponse(
        Long id,
        String particular,
        BigDecimal amount,
        String createdBy
) {}
