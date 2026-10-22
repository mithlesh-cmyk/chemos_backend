package chemos.chem_os.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"parentRole", "permissions", "inheritedPermissions"})
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

    // The parent link alone does not grant permissions. Inheritance is opt-in and
    // includes only the direct parent's own permissions, never a super role's.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_role_id")
    private Role parentRole;

    public enum InheritanceMode { NONE, ALL, SELECTED }

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'NONE'")
    @Column(name = "inheritance_mode", nullable = false, length = 16)
    private InheritanceMode inheritanceMode = InheritanceMode.NONE;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_inherited_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> inheritedPermissions = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    // True for roles whose users may only see/act on records they created themselves
    // (e.g. SALES_EXECUTIVE, PURCHASE_EXECUTIVE) — enforced in the relevant services via
    // CurrentUserService.isRowScoped(), not here. Managers/Director/Admin leave this false.
    @Column(name = "restrict_to_own_records", nullable = false,
            columnDefinition = "boolean not null default false")
    private boolean restrictToOwnRecords = false;
}
