package chemos.chem_os.dto;

import java.util.List;

/**
 * Full summary for a sale: its total requirement, how much is already linked to POs,
 * how much still needs to be fulfilled, and the individual PO links.
 */
public record SaleLinkSummaryResponse(
        String saleId,
        Double totalRequired,       // = sale.quantity (fixed)
        Double totalLinked,         // = SUM(linked_quantity for all links of this sale)
        Double remaining,           // = totalRequired - totalLinked
        List<LinkedPurchaseItem> links
) {
    /**
     * One PO linked to this sale.
     */
    public record LinkedPurchaseItem(
            String linkId,
            String purchaseId,
            Double linkedQuantity,              // amount from this PO committed to this sale
            Double purchaseOriginalQuantity,    // full / original PO quantity
            Double purchaseAvailableQuantity    // remaining available from this PO across all sales
    ) {}
}
