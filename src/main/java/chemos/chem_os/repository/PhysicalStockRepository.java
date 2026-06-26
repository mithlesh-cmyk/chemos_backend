package chemos.chem_os.repository;

import chemos.chem_os.model.PhysicalStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhysicalStockRepository extends JpaRepository<PhysicalStock, String> {

    Optional<PhysicalStock> findByPurchaseId(String purchaseId);
}
