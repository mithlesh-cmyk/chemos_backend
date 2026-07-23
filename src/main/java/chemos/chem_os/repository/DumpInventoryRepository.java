package chemos.chem_os.repository;

import chemos.chem_os.model.DumpInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DumpInventoryRepository extends JpaRepository<DumpInventory, UUID> {

}