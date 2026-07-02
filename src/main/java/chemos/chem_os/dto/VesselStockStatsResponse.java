package chemos.chem_os.dto;

public record VesselStockStatsResponse(
        String vesselName,
        String product,
        String port,
        Double physicalStockOpening,
        Double physicalSold,
        Double physicalUnsoldClosing,
        Double incomingUnsoldOpening,
        Double incomingUnsoldNew,
        Double incomingSold,
        Double incomingUnsoldClosing,
        Double totalStock
) {
}
