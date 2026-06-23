package chemos.chem_os.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// Deny-list per user. Presence of a row means the user is DENIED that permission,
// even if their role hierarchy would otherwise grant it.
// This is never used to grant permissions — only to restrict them.
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

    @Column(name = "reason")
    private String reason; // audit: why was this permission restricted?

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restricted_by")
    private User restrictedBy; // audit: which admin set this restriction

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
