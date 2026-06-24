package chemos.chem_os.dto;

public record UpdateSaleRequest(
        String salesType,
        String companyTo,
        String companyFrom,
        String product,
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
        String remarks
) {}