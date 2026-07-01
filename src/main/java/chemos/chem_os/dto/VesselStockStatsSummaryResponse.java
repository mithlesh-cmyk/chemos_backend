package chemos.chem_os.dto;

public record VesselStockStatsSummaryResponse(
        Double totalStock,
        Double physicalUnsoldClosing,
        Double incomingUnsoldClosing,
        Double incomingSold
) {
}
