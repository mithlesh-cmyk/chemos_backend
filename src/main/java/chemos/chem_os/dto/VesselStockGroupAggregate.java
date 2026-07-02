package chemos.chem_os.dto;

public record VesselStockGroupAggregate(
        String vesselName,
        String product,
        String port,
        Double total
) {
}
