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
    //
    // Inheritance here is strictly additive — a child role always ends up with AT LEAST
    // everything its parent has, never less. So parentRole must only ever point at a role
    // whose entire permission set you're happy to have fully absorbed into the child.
    // Org-chart "reports to" relationships are NOT the same thing as parentRole — a role
    // one level down in the business hierarchy should usually have parentRole = null and
    // its own explicit (narrower) permission set, not inherit from its manager's role.
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

    // True for roles whose users may only see/act on records they created themselves
    // (e.g. SALES_EXECUTIVE, PURCHASE_EXECUTIVE) — enforced in the relevant services via
    // CurrentUserService.isRowScoped(), not here. Managers/Director/Admin leave this false.
    @Column(name = "restrict_to_own_records", nullable = false,
            columnDefinition = "boolean not null default false")
    private boolean restrictToOwnRecords = false;
}
