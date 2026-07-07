package chemos.chem_os.dto;

public record CreateSaleRequest(
        String salesType,
        
        String companyTo,

        String companyFrom,
        
        String productId,
        
        Double quantity,
        
        Double price,
        
        String payment,
        
        String deliveryTerm,
        
        String port,
        
        Double marketPrice,
        
        String marketStatus,
        
        Integer storageDays,
        
        String make,
        
        String packaging,
        
        String origin,
        
        String transitTolerance,
        
        String message,

        String vesselName,

        String remarks,

        String salesPerson,

        String brokerName
) {
}
