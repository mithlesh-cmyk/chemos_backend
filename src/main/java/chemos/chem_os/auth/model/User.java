package chemos.chem_os.auth.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Data
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @Column(name = "id")
    private UUID id;

    private String username;

    private String password;

    private Boolean isActive;
}
