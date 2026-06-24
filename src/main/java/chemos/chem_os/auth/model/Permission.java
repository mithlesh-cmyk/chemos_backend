package chemos.chem_os.auth.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString
@Table(name = "permissions")
public class Permission {

    @Id
    @Column(name = "id")
    private String id; // "sale_view", "pur_view", etc.

    @Column(name = "permission_code", unique = true, nullable = false)
    private String permissionCode; // "SALE_VIEW", "PURCHASE_VIEW", etc.

    @Column(name = "display_name", nullable = false)
    private String displayName; // "View Sales", "View Purchases", etc.

    @Column(name = "module", nullable = false)
    private String module; // "SALES", "PURCHASES", "COMPANY", "PRODUCTS", "ADMIN"
}
