package chemos.chem_os.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// Per-user permission override, on top of whatever the user's role grants.
// effect=DENY: the user is denied this permission even if their role would otherwise grant it.
// effect=ALLOW: the user is granted this permission even though their role does not include it
// (e.g. a manager delegating one specific permission to one specific executive).
// See PermissionResolverService.resolve() for how these combine with role permissions.
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"user", "restrictedBy"})
@Table(
        name = "user_permission_restrictions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "permission_id"})
)
public class UserPermissionRestriction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    public enum OverrideEffect { ALLOW, DENY }

    @Enumerated(EnumType.STRING)
    @Column(name = "effect", nullable = false)
    private OverrideEffect effect = OverrideEffect.DENY;

    @Column(name = "reason")
    private String reason; // audit: why was this permission granted/restricted?

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restricted_by")
    private User restrictedBy; // audit: which admin set this restriction

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
