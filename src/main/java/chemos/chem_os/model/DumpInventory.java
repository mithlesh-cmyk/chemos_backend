package chemos.chem_os.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dump_inventory")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DumpInventory {

    @Id
    @GeneratedValue
    private UUID id;

    private String product;

    private String port;

    private String company;

    private Double physicalStock;

    private Double purchaseReady;

    private Double physicalSold;

    private Double physicalUnsold;

    private Double incomingStock;

    private Double purchaseIncoming;

    private Double incomingSales;

    private Double incomingBalance;

    private Double totalStock;

    private LocalDateTime lastCsvUploadedAt;

    private LocalDateTime updatedAt;
}