package chemos.chem_os.repository;

import chemos.chem_os.model.EntryStatus;
import chemos.chem_os.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, String> {

    List<Purchase> findByStatus(EntryStatus status);
}
