package chemos.chem_os.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Permanent, append-only record of every create/update that pushed a
 * SalePurchaseLink's purchase-side quantity negative. Rows here are never
 * updated or deleted alongside the link they reference, so a link that was
 * later corrected (or even deleted) still leaves a trace of when it went
 * negative, by how much, and who did it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "sale_purchase_link_negative_history")
public class SalePurchaseLinkNegativeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @Column(name = "link_id", nullable = false)
    private String linkId;

    @Column(name = "sale_id", nullable = false)
    private String saleId;

    @Column(name = "purchase_id", nullable = false)
    private String purchaseId;

    @Column(name = "linked_quantity", nullable = false)
    private Double linkedQuantity;

    @Column(name = "purchase_original_quantity", nullable = false)
    private Double purchaseOriginalQuantity;

    @Column(name = "purchase_available_quantity", nullable = false)
    private Double purchaseAvailableQuantity;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "changed_by_username", nullable = false)
    private String changedByUsername;

    @CreationTimestamp
    @Column(name = "occurred_at", updatable = false)
    private LocalDateTime occurredAt;
}
