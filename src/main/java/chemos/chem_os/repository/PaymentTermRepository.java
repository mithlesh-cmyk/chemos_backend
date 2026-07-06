package chemos.chem_os.repository;

import chemos.chem_os.model.PaymentTerms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PaymentTermRepository extends JpaRepository<PaymentTerms, Integer> {

    Optional<PaymentTerms> findByPaymentTermIgnoreCase(String paymentTerm);

    @Query("SELECT p FROM PaymentTerms p WHERE p.isActive = true")
    List<PaymentTerms> findAllActive();
}
