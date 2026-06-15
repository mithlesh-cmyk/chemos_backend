package chemos.chem_os.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@Table(name = "companies")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Companies {

    @Id
    @Generated(event = EventType.INSERT)
    private String id;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "search_key")
    private String searchKey;

    @Column(name = "created_at")
    private LocalDate createdAt;
}
