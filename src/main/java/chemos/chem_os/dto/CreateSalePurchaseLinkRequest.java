package chemos.chem_os.dto;

public record CreateSalePurchaseLinkRequest(
        String saleId,
        String purchaseId,
        Double linkedQuantity
) {}
