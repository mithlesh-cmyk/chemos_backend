package chemos.chem_os.repository;

import chemos.chem_os.dto.VesselGroupCompany;
import chemos.chem_os.dto.VesselStockGroupAggregate;
import chemos.chem_os.model.EntryStatus;
import chemos.chem_os.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, String>, JpaSpecificationExecutor<Purchase> {

    List<Purchase> findByStatus(EntryStatus status);

    @Query("""
        SELECT new chemos.chem_os.dto.VesselStockGroupAggregate(
            UPPER(TRIM(p.vesselName)), UPPER(TRIM(p.product.name)), UPPER(TRIM(p.dischargePort.displayName)), COALESCE(SUM(p.quantity), 0))
        FROM Purchase p
        WHERE p.marketStatus = 'Incoming'
          AND p.status = chemos.chem_os.model.EntryStatus.CONFIRMED
          AND CAST(p.createdAt AS date) = :onDate
        GROUP BY UPPER(TRIM(p.vesselName)), UPPER(TRIM(p.product.name)), UPPER(TRIM(p.dischargePort.displayName))
        """)
    List<VesselStockGroupAggregate> sumIncomingNewByGroup(@Param("onDate") LocalDate onDate);

    @Query("""
        SELECT DISTINCT new chemos.chem_os.dto.VesselGroupCompany(
            UPPER(TRIM(p.vesselName)), UPPER(TRIM(p.product.name)), UPPER(TRIM(p.dischargePort.displayName)), TRIM(p.companyTo))
        FROM Purchase p
        WHERE p.companyTo IS NOT NULL
          AND p.status = chemos.chem_os.model.EntryStatus.CONFIRMED
        """)
    List<VesselGroupCompany> findCompanyToByGroup();
}
