package chemos.chem_os.dto;

public record ProductStockBreakdownResponse(
        String product,
        String dischargePort,
        Double physicalReady,
        Double physicalStock,
        Double physicalSold,
        Double physicalUnsold,
        Double incomingStock,
        Double purchaseIncoming,
        Double incomingSales,
        Double incomingBalance,
        Double totalStock,
        String companyName

) {
}
