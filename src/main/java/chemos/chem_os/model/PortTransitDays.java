package chemos.chem_os.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Table(name = "port_transit_days")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortTransitDays {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_port_id", nullable = false)
    private Ports fromPort;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_port_id", nullable = false)
    private Ports toPort;

    @Column(nullable = false)
    private Integer days;
}
