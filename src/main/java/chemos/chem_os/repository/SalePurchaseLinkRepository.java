package chemos.chem_os.repository;

import chemos.chem_os.model.SalePurchaseLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SalePurchaseLinkRepository extends JpaRepository<SalePurchaseLink, String> {

    List<SalePurchaseLink> findBySaleId(String saleId);

    List<SalePurchaseLink> findByPurchaseId(String purchaseId);

    boolean existsBySaleIdAndPurchaseId(String saleId, String purchaseId);

    /**
     * Total quantity already committed from a given purchase across all sales.
     */
    @Query("SELECT COALESCE(SUM(l.linkedQuantity), 0) FROM SalePurchaseLink l WHERE l.purchaseId = :purchaseId")
    Double sumLinkedQuantityByPurchaseId(@Param("purchaseId") String purchaseId);

    /**
     * Total quantity already fulfilled for a given sale across all purchases.
     */
    @Query("SELECT COALESCE(SUM(l.linkedQuantity), 0) FROM SalePurchaseLink l WHERE l.saleId = :saleId")
    Double sumLinkedQuantityBySaleId(@Param("saleId") String saleId);
}
