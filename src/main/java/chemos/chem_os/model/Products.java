package chemos.chem_os.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "products")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Products {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "hs_code")
    private String hsCode;

    @Column(name = "cas_no")
    private String casNo;
}
