package chemos.chem_os.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record VesselStockStatsResponse(
        String vesselName,
        String product,
        String dischargePort,
        Double physicalStockOpening,
        Double physicalSold,
        Double physicalUnsoldClosing,
        Double incomingUnsoldOpening,
        Double incomingUnsoldNew,
        Double incomingSold,
        Double incomingUnsoldClosing,
        Double totalStock,
        String companyName,
        BigDecimal marketPrice,
        BigDecimal replacementCost,
        LocalDateTime date,
        LocalDate vesselDate
) {
}
