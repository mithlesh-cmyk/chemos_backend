package chemos.chem_os.dto;

import java.util.List;

/**
 * Full summary for a purchase: its original quantity, how much is already committed to sales,
 * how much is still available, and the individual sale links.
 */
public record PurchaseLinkSummaryResponse(
        String purchaseId,
        Double originalQuantity,    // = purchase.quantity (fixed, never changes)
        Double totalLinked,         // = SUM(linked_quantity for all links of this PO)
        Double availableQuantity,   // = originalQuantity - totalLinked
        List<LinkedSaleItem> links
) {
    /**
     * One sale linked to this purchase.
     */
    public record LinkedSaleItem(
            String linkId,
            String saleId,
            Double linkedQuantity,          // amount from this PO committed to this sale
            Double saleTotalRequired,       // full sale requirement
            Double saleRemainingQuantity    // remaining unfulfilled portion of this sale
    ) {}
}
