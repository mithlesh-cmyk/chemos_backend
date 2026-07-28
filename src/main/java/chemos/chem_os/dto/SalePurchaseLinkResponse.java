package chemos.chem_os.dto;

import java.time.LocalDateTime;

/**
 * Returned after create/update of a single link.
 * Includes the derived available/remaining quantities computed at the time of the call.
 */
public record SalePurchaseLinkResponse(
        String id,
        String saleId,
        String purchaseId,
        String createdByUsername,
        String updatedBy,
        Double linkedQuantity,
        Boolean negative,                   // true if linkedQuantity ever exceeded the PO's available quantity

        // Purchase context
        Double purchaseOriginalQuantity,
        Double purchaseAvailableQuantity,   // purchase.quantity - SUM(all linked for this PO); can be negative

        // Sale context
        Double saleTotalRequired,
        Double saleRemainingQuantity,       // sale.quantity - SUM(all linked for this sale)

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
