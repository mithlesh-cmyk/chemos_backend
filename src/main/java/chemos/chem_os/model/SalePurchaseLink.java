package chemos.chem_os.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(
    name = "sale_purchase_links",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_sale_purchase",
        columnNames = {"sale_id", "purchase_id"}
    )
)
public class SalePurchaseLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @Column(name = "sale_id", nullable = false)
    private String saleId;

    @Column(name = "purchase_id", nullable = false)
    private String purchaseId;

    @Column(name = "created_by_username", nullable = false)
    private String createdByUsername;

    @Column(name = "updated_by")
    private String updatedBy;

    /**
     * The quantity (in MT) allocated from this purchase to fulfil this sale.
     * This is the actual amount committed — not the full PO or sale quantity.
     */
    @Column(name = "linked_quantity", nullable = false)
    private Double linkedQuantity;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
