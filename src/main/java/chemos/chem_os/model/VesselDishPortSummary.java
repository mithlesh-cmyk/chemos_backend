package chemos.chem_os.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vessel_dish_port_summary_csv", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class VesselDishPortSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product", length = 255)
    private String product;

    @Column(name = "\"vessel name\"", length = 255)
    private String vesselName;

    @Column(name = "\"dish port clean\"", length = 255)
    private String dishPortClean;

    @Column(name = "\"total qty\"", precision = 18, scale = 3)
    private BigDecimal totalQty;

    @Column(name = "\"shipment count\"")
    private Integer shipmentCount;

    @Column(name = "\"avg waiting days\"", precision = 10, scale = 2)
    private BigDecimal avgWaitingDays;

    @Column(name = "\"avg berth days\"", precision = 10, scale = 2)
    private BigDecimal avgBerthDays;

    @Column(name = "\"dish port share\"", precision = 8, scale = 4)
    private BigDecimal dishPortShare;

    @Column(name = "\"ship id\"")
    private Long shipId;

    @Column(name = "\"product id\"", length = 255)
    private String productId;

    @Column(name = "\"port id\"", length = 50)
    private String portId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "\"product id\"", referencedColumnName = "id",
            insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_vessel_dish_port_summary_product"))
    private Products productReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "\"port id\"", referencedColumnName = "id",
            insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_vessel_dish_port_summary_port"))
    private Ports portReference;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
