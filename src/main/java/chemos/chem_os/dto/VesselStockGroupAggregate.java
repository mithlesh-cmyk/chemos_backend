package chemos.chem_os.dto;

public record VesselStockGroupAggregate(
        String vesselName,
        String product,
        String dischargePort,
        Double total
) {
}
