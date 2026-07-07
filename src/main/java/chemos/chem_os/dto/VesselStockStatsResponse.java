package chemos.chem_os.dto;

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
        String companyName
) {
}
