package chemos.chem_os.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(
        name = "incoming_unsold_snapshots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_incoming_unsold_snapshot_group",
                columnNames = {"snapshot_date", "vessel_name", "product", "port"}
        )
)
public class IncomingUnsoldSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "vessel_name", nullable = false)
    private String vesselName;

    @Column(name = "product", nullable = false)
    private String product;

    @Column(name = "port", nullable = false)
    private String port;

    @Column(name = "incoming_unsold_opening", nullable = false)
    private Double incomingUnsoldOpening;

    @Column(name = "incoming_unsold_new", nullable = false)
    private Double incomingUnsoldNew;

    @Column(name = "incoming_sold", nullable = false)
    private Double incomingSold;

    @Column(name = "incoming_unsold_closing", nullable = false)
    private Double incomingUnsoldClosing;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;
}
