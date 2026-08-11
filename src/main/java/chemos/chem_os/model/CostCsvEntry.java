package chemos.chem_os.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "cost_csv_entries")
public class CostCsvEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "particular", nullable = false)
    private String particular;

    @Column(name = "direct_cost", precision = 20, scale = 2, nullable = false)
    private BigDecimal directCost;

    @Column(name = "indirect_cost", precision = 20, scale = 2, nullable = false)
    private BigDecimal indirectCost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_id", nullable = false)
    private CostCsvUpload upload;

    @Column(name = "created_by")
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
