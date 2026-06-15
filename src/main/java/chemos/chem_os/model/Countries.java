package chemos.chem_os.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@Table(name = "countries")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Countries {

    @Id
    private String id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "search_key", nullable = false, unique = true)
    private String searchKey;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "updated_at")
    private LocalDate updatedAt;
}
