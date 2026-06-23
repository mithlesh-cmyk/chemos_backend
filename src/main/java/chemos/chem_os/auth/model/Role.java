package chemos.chem_os.auth.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"parentRole", "permissions"})
@Table(name = "roles")
public class Role {

    @Id
    @Column(name = "id")
    private String id; // "admin", "pur_man", "sal_man", etc.

    @Column(name = "display_name")
    private String displayName; // "Administrator", "Purchase Manager", etc.

    @Column(name = "name", unique = true, nullable = false)
    private String name; // "ADMIN", "PURCHASE_MANAGER", "SALES_MANAGER", etc.

    // True for roles like ADMIN that bypass the permission table and always get all permissions.
    // Used to support audit logging: every action ties to a real permission code,
    // even for admin, since the resolver injects all codes into the security context.
    @Column(name = "is_super_role", nullable = false)
    private boolean superRole = false;

    // Direct parent in the role hierarchy. Permission inheritance is 1 level up only:
    // effective = own permissions ∪ parent's permissions.
    // Super roles are skipped during inheritance traversal.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_role_id")
    private Role parentRole;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
}
