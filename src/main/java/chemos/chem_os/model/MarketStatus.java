package chemos.chem_os.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Table(name = "market_statuses")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketStatus {

    @Id
    private String id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;
}