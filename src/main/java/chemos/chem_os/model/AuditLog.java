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
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "performed_by", nullable = false)
    private String performedBy;

    @Column(name = "performed_by_name")
    private String performedByName;

    @Column(name = "performed_by_role")
    private String performedByRole;

    @Column(name = "data_before", columnDefinition = "TEXT")
    private String dataBefore;

    @Column(name = "data_after", columnDefinition = "TEXT")
    private String dataAfter;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;
}
