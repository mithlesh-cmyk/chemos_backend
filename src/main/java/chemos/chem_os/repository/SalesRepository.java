package chemos.chem_os.repository;

import chemos.chem_os.dto.VesselStockGroupAggregate;
import chemos.chem_os.model.Sales;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SalesRepository extends JpaRepository<Sales, String>, JpaSpecificationExecutor<Sales> {

    @Query("SELECT s FROM Sales s WHERE " +
            "(:productId IS NULL OR s.product.id = :productId) AND " +
            "(:companyTo IS NULL OR s.companyTo = :companyTo) AND " +
            "(:port IS NULL OR s.port = :port) AND " +
            "s.date >= :startDate AND s.date <= :endDate")
    Page<Sales> findWithFilters(
            @Param("productId") String productId,
            @Param("companyTo") String companyTo,
            @Param("port") String port,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("""
        SELECT new chemos.chem_os.dto.VesselStockGroupAggregate(
            UPPER(TRIM(s.vesselName)), UPPER(TRIM(s.product)), UPPER(TRIM(s.port)), COALESCE(SUM(s.quantity), 0))
        FROM Sales s
        WHERE s.marketStatus = 'Ready Market'
          AND s.date = :onDate
          AND s.status = chemos.chem_os.model.EntryStatus.CONFIRMED
        GROUP BY UPPER(TRIM(s.vesselName)), UPPER(TRIM(s.product)), UPPER(TRIM(s.port))
        """)
    List<VesselStockGroupAggregate> sumReadyMarketSoldByGroup(@Param("onDate") LocalDate onDate);

    @Query("""
        SELECT new chemos.chem_os.dto.VesselStockGroupAggregate(
            UPPER(TRIM(s.vesselName)), UPPER(TRIM(s.product)), UPPER(TRIM(s.port)), COALESCE(SUM(s.quantity), 0))
        FROM Sales s
        WHERE s.marketStatus = 'Incoming'
          AND s.date = :onDate
          AND s.status = chemos.chem_os.model.EntryStatus.CONFIRMED
        GROUP BY UPPER(TRIM(s.vesselName)), UPPER(TRIM(s.product)), UPPER(TRIM(s.port))
        """)
    List<VesselStockGroupAggregate> sumIncomingSoldByGroup(@Param("onDate") LocalDate onDate);
}