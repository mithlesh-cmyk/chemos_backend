package chemos.chem_os.dto;

import java.time.LocalDateTime;

/**
 * One permanent record of a create/update that pushed a link's purchase-side
 * quantity negative. Independent of the link's current state — stays around
 * even after the link is corrected or deleted, for historical tracking.
 */
public record SalePurchaseLinkNegativeHistoryResponse(
        String id,
        String linkId,
        String saleId,
        String purchaseId,
        Double linkedQuantity,
        Double purchaseOriginalQuantity,
        Double purchaseAvailableQuantity,   // negative value at the time of this event
        String action,                      // CREATE or UPDATE
        String changedByUsername,
        LocalDateTime occurredAt
) {}
