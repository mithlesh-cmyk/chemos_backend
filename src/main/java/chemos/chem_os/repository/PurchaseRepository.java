package chemos.chem_os.repository;

import chemos.chem_os.model.EntryStatus;
import chemos.chem_os.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, String>, JpaSpecificationExecutor<Purchase> {

    List<Purchase> findByStatus(EntryStatus status);
}
