package chemos.chem_os.repository;

import chemos.chem_os.dto.VesselGroupCompany;
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
            UPPER(TRIM(s.vesselName)), UPPER(TRIM(s.product.name)), UPPER(TRIM(port.displayName)), COALESCE(SUM(s.quantity), 0))
        FROM Sales s
        LEFT JOIN s.port port
        WHERE s.marketStatus = 'ready'
          AND s.date <= :onDate
          AND s.status.id = 'CONFIRMED'
        GROUP BY UPPER(TRIM(s.vesselName)), UPPER(TRIM(s.product.name)), UPPER(TRIM(port.displayName))
        """)
    List<VesselStockGroupAggregate> sumReadyMarketSoldByGroup(@Param("onDate") LocalDate onDate);

    @Query("""
        SELECT new chemos.chem_os.dto.VesselStockGroupAggregate(
            UPPER(TRIM(s.vesselName)), UPPER(TRIM(s.product.name)), UPPER(TRIM(port.displayName)), COALESCE(SUM(s.quantity), 0))
        FROM Sales s
        LEFT JOIN s.port port
        WHERE s.marketStatus = 'ready'
          AND s.status.id = 'CONFIRMED'
        GROUP BY UPPER(TRIM(s.vesselName)), UPPER(TRIM(s.product.name)), UPPER(TRIM(port.displayName))
        """)
    List<VesselStockGroupAggregate> sumReadyMarketSoldAllTimeByGroup();

    @Query("""
    SELECT new chemos.chem_os.dto.VesselStockGroupAggregate(
        UPPER(TRIM(s.vesselName)), UPPER(TRIM(s.product.name)), UPPER(TRIM(port.displayName)), COALESCE(SUM(s.quantity), 0))
    FROM Sales s
    LEFT JOIN s.port port
    WHERE s.marketStatus = 'incoming'
      AND CAST(s.confirmedAt AS date) = :onDate
      AND s.status.id = 'CONFIRMED'
    GROUP BY UPPER(TRIM(s.vesselName)), UPPER(TRIM(s.product.name)), UPPER(TRIM(port.displayName))
    """)
    List<VesselStockGroupAggregate> sumIncomingSoldByGroup(@Param("onDate") LocalDate onDate);

    @Query("""
        SELECT new chemos.chem_os.dto.VesselStockGroupAggregate(
            UPPER(TRIM(s.vesselName)), UPPER(TRIM(s.product.name)), UPPER(TRIM(port.displayName)), COALESCE(SUM(s.quantity), 0))
        FROM Sales s
        LEFT JOIN s.port port
        WHERE s.marketStatus = 'incoming'
          AND s.status.id = 'CONFIRMED'
        GROUP BY UPPER(TRIM(s.vesselName)), UPPER(TRIM(s.product.name)), UPPER(TRIM(port.displayName))
        """)
    List<VesselStockGroupAggregate> sumIncomingSoldAllTimeByGroup();

    @Query("""
        SELECT COALESCE(SUM(s.quantity), 0)
        FROM Sales s
        LEFT JOIN s.port port
        WHERE s.marketStatus = 'incoming'
          AND s.status.id = 'CONFIRMED'
          AND UPPER(TRIM(s.vesselName)) = :vesselName
          AND UPPER(TRIM(s.product.name)) = :product
          AND UPPER(TRIM(port.displayName)) = :port
          AND CAST(s.confirmedAt AS date) < :beforeDate
        """)
    double sumIncomingConfirmedBefore(@Param("vesselName") String vesselName,
                                       @Param("product") String product,
                                       @Param("port") String port,
                                       @Param("beforeDate") LocalDate beforeDate);

    @Query("""
        SELECT DISTINCT new chemos.chem_os.dto.VesselGroupCompany(
            UPPER(TRIM(s.vesselName)), UPPER(TRIM(s.product.name)), UPPER(TRIM(port.displayName)), TRIM(s.companyFrom))
        FROM Sales s
        LEFT JOIN s.port port
        WHERE s.companyFrom IS NOT NULL
          AND s.status.id = 'CONFIRMED'
        """)
    List<VesselGroupCompany> findCompanyFromByGroup();
}