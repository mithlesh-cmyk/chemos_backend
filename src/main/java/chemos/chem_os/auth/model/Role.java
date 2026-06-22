package chemos.chem_os.auth.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Table(name = "roles")
@AllArgsConstructor
@NoArgsConstructor
public class Role {

    @Id
    @Column(name = "id")
    private String id; // "admin", "pur_man", "pur_exe", etc.

    @Column(name = "display_name")
    private String displayName; // "Administrator", "Purchase Manager", etc.

    @Column(name = "name", unique = true, nullable = false)
    private String name; // "ADMIN", "PURCHASE_MANAGER", "PURCHASE_EXECUTIVE", etc.
}
