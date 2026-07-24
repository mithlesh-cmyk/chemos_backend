package chemos.chem_os.repository;

import chemos.chem_os.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    @Query("""
    SELECT MAX(i.lastCsvUploadedAt)
    FROM Inventory i
    """)
    LocalDateTime getLastCsvUploadedAt();

}