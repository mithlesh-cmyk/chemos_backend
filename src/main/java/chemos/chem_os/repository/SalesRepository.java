package chemos.chem_os.repository;

import chemos.chem_os.model.Sales;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesRepository extends JpaRepository<Sales, String> {
}
