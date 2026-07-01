package chemos.chem_os.dto;

import java.util.List;

public record ProductStockBreakdownResponse(
        String product,
        String port,
        Double physicalStockOpening,
        Double physicalSold,
        Double physicalUnsoldClosing,
        Double incomingUnsoldOpening,
        Double incomingUnsoldNew,
        Double incomingSold,
        Double incomingUnsoldClosing,
        Double totalStock,
        List<VesselInventoryDetail> vesselInventory
) {
}
