package chemos.chem_os.repository;

import chemos.chem_os.dto.PhysicalStockSessionSummary;
import chemos.chem_os.dto.VesselInventoryRow;
import chemos.chem_os.dto.VesselStockGroupAggregate;
import chemos.chem_os.model.PhysicalStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PhysicalStockRepository extends JpaRepository<PhysicalStock, String> {

    Optional<PhysicalStock> findByPurchaseId(String purchaseId);

    List<PhysicalStock> findByUpdatedByAndUpdatedAt(String updatedBy, LocalDateTime updatedAt);

    @Query("""
        SELECT new chemos.chem_os.dto.PhysicalStockSessionSummary(
            p.updatedBy, p.updatedAt, COUNT(p.id))
        FROM PhysicalStock p
        WHERE p.updatedBy IS NOT NULL
        GROUP BY p.updatedBy, p.updatedAt
        ORDER BY p.updatedAt DESC
        """)
    List<PhysicalStockSessionSummary> findSessionSummaries();

    @Query("""
        SELECT new chemos.chem_os.dto.PhysicalStockSessionSummary(
            p.updatedBy, p.updatedAt, COUNT(p.id))
        FROM PhysicalStock p
        WHERE p.updatedBy = :user
        GROUP BY p.updatedBy, p.updatedAt
        ORDER BY p.updatedAt DESC
        """)
    List<PhysicalStockSessionSummary> findSessionSummariesByUser(@Param("user") String user);

    @Query("""
        SELECT new chemos.chem_os.dto.VesselStockGroupAggregate(
            UPPER(TRIM(p.vesselName)), UPPER(TRIM(p.product)), UPPER(TRIM(p.dischargePort.displayName)), COALESCE(SUM(ps.physicalStock), 0))
        FROM PhysicalStock ps
        JOIN Purchase p ON p.id = ps.purchaseId
        WHERE p.status = chemos.chem_os.model.EntryStatus.CONFIRMED
        GROUP BY UPPER(TRIM(p.vesselName)), UPPER(TRIM(p.product)), UPPER(TRIM(p.dischargePort.displayName))
        """)
    List<VesselStockGroupAggregate> sumPhysicalStockOpeningByGroup();

    @Query("""
        SELECT new chemos.chem_os.dto.VesselInventoryRow(
            UPPER(TRIM(p.vesselName)), UPPER(TRIM(p.product)), UPPER(TRIM(p.dischargePort.displayName)), ps.updatedAt, p.companyFrom)
        FROM PhysicalStock ps
        JOIN Purchase p ON p.id = ps.purchaseId
        WHERE p.status = chemos.chem_os.model.EntryStatus.CONFIRMED
        """)
    List<VesselInventoryRow> findVesselInventoryRows();
}