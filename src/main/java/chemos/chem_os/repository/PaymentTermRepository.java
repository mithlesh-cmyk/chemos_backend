package chemos.chem_os.repository;

import chemos.chem_os.model.PaymentTerms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PaymentTermRepository extends JpaRepository<PaymentTerms, Integer> {

    @Query("SELECT p FROM PaymentTerms p WHERE p.isActive = true")
    List<PaymentTerms> findAllActive();
}
