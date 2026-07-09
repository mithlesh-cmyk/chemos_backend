package chemos.chem_os.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "salespersons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)

public class Salespersons {
    @Id
    @Column(nullable = false)
    private String id;

    @Column(nullable= false)
    private String name;
}

