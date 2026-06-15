package chemos.chem_os.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "ports")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Ports {

    @Id
    @Generated(event = EventType.INSERT)
    private String id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "search_key", nullable = false, unique = true)
    private String searchKey;

    @Column(name = "locode")
    private String locode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}