package chemos.chem_os.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "physical_stocks")
public class PhysicalStock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "purchase_id", nullable = false, unique = true)
    private String purchaseId;

    @Column(name = "physical_stock")
    private Double physicalStock;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
