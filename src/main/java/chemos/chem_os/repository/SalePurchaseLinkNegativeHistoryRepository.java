package chemos.chem_os.repository;

import chemos.chem_os.model.SalePurchaseLinkNegativeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalePurchaseLinkNegativeHistoryRepository extends JpaRepository<SalePurchaseLinkNegativeHistory, String> {

    List<SalePurchaseLinkNegativeHistory> findAllByOrderByOccurredAtDesc();

    List<SalePurchaseLinkNegativeHistory> findByLinkIdOrderByOccurredAtDesc(String linkId);
}
