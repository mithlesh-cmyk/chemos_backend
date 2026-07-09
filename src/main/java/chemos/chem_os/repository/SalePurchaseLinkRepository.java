package chemos.chem_os.repository;

import chemos.chem_os.model.SalePurchaseLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SalePurchaseLinkRepository extends JpaRepository<SalePurchaseLink, String> {

    List<SalePurchaseLink> findBySaleId(String saleId);

    List<SalePurchaseLink> findByPurchaseId(String purchaseId);

    List<SalePurchaseLink> findByCreatedByUsernameOrderByCreatedAtDesc(String createdByUsername);

    boolean existsBySaleIdAndPurchaseId(String saleId, String purchaseId);

    /**
     * Total quantity already committed from a given purchase across all sales.
     * Excludes links whose sale has been cancelled, since a cancelled sale no
     * longer holds any of the purchase's quantity.
     */
    @Query("""
        SELECT COALESCE(SUM(l.linkedQuantity), 0)
        FROM SalePurchaseLink l
        JOIN Sales s ON s.id = l.saleId
        WHERE l.purchaseId = :purchaseId
          AND s.status.id <> 'CANCELLED'
        """)
    Double sumLinkedQuantityByPurchaseId(@Param("purchaseId") String purchaseId);

    /**
     * Total quantity already fulfilled for a given sale across all purchases.
     * Excludes links whose purchase has been cancelled, since a cancelled
     * purchase no longer fulfils any of the sale's quantity.
     */
    @Query("""
        SELECT COALESCE(SUM(l.linkedQuantity), 0)
        FROM SalePurchaseLink l
        JOIN Purchase p ON p.id = l.purchaseId
        WHERE l.saleId = :saleId
          AND p.status.id <> 'CANCELLED'
        """)
    Double sumLinkedQuantityBySaleId(@Param("saleId") String saleId);
}
