package chemos.chem_os.repository;

import chemos.chem_os.dto.VesselGroupCompany;
import chemos.chem_os.dto.VesselStockGroupAggregate;
import chemos.chem_os.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, String>, JpaSpecificationExecutor<Purchase> {

    List<Purchase> findByStatus_Id(String statusId);

    @Query("""
        SELECT new chemos.chem_os.dto.VesselStockGroupAggregate(
            UPPER(TRIM(p.vesselName)), UPPER(TRIM(p.product.name)), UPPER(TRIM(p.dischargePort.displayName)), COALESCE(SUM(p.quantity), 0))
        FROM Purchase p
        WHERE p.marketStatus = 'incoming'
          AND p.status.id = 'CONFIRMED'
          AND CAST(p.confirmedAt AS date) = :onDate
                    AND p.isDeleted = false
        GROUP BY UPPER(TRIM(p.vesselName)), UPPER(TRIM(p.product.name)), UPPER(TRIM(p.dischargePort.displayName))
        """)
    List<VesselStockGroupAggregate> sumIncomingNewByGroup(@Param("onDate") LocalDate onDate);

    @Query("""
        SELECT new chemos.chem_os.dto.VesselStockGroupAggregate(
            UPPER(TRIM(p.vesselName)), UPPER(TRIM(p.product.name)), UPPER(TRIM(p.dischargePort.displayName)), COALESCE(SUM(p.quantity), 0))
        FROM Purchase p
        WHERE p.marketStatus = 'incoming'
          AND p.status.id = 'CONFIRMED'
<<<<<<< Updated upstream
          AND p.confirmedAt > :after
        GROUP BY UPPER(TRIM(p.vesselName)), UPPER(TRIM(p.product.name)), UPPER(TRIM(p.dischargePort.displayName))
        """)
    List<VesselStockGroupAggregate> sumIncomingNewAfter(@Param("after") LocalDateTime after);

    @Query("""
        SELECT new chemos.chem_os.dto.VesselStockGroupAggregate(
            UPPER(TRIM(p.vesselName)), UPPER(TRIM(p.product.name)), UPPER(TRIM(p.dischargePort.displayName)), COALESCE(SUM(p.quantity), 0))
        FROM Purchase p
        WHERE p.marketStatus = 'incoming'
          AND p.status.id = 'CONFIRMED'
=======
                  AND p.isDeleted = false
>>>>>>> Stashed changes
        GROUP BY UPPER(TRIM(p.vesselName)), UPPER(TRIM(p.product.name)), UPPER(TRIM(p.dischargePort.displayName))
        """)
    List<VesselStockGroupAggregate> sumIncomingAllTimeByGroup();

    @Query("""
        SELECT COALESCE(SUM(p.quantity), 0)
        FROM Purchase p
        WHERE p.marketStatus = 'incoming'
          AND p.status.id = 'CONFIRMED'
          AND UPPER(TRIM(p.vesselName)) = :vesselName
          AND UPPER(TRIM(p.product.name)) = :product
          AND UPPER(TRIM(p.dischargePort.displayName)) = :port
          AND CAST(p.confirmedAt AS date) < :beforeDate
                  AND p.isDeleted = false
        """)
    double sumIncomingConfirmedBefore(@Param("vesselName") String vesselName,
                                      @Param("product") String product,
                                      @Param("port") String port,
                                      @Param("beforeDate") LocalDate beforeDate);

    @Query("""
        SELECT DISTINCT new chemos.chem_os.dto.VesselGroupCompany(
            UPPER(TRIM(p.vesselName)), UPPER(TRIM(p.product.name)), UPPER(TRIM(p.dischargePort.displayName)), TRIM(p.companyTo))
        FROM Purchase p
        WHERE p.companyTo IS NOT NULL
          AND p.status.id = 'CONFIRMED'
                  AND p.isDeleted = false
        """)
    List<VesselGroupCompany> findCompanyToByGroup();

    @Query("""
SELECT new chemos.chem_os.dto.VesselStockGroupAggregate(
    UPPER(TRIM(p.vesselName)),
    UPPER(TRIM(p.product.name)),
    UPPER(TRIM(p.dischargePort.displayName)),
    COALESCE(SUM(p.quantity), 0)
)
FROM Purchase p
WHERE LOWER(TRIM(p.marketStatus)) = 'ready'
  AND p.status.id = 'CONFIRMED'
  AND p.isDeleted = false
GROUP BY
    UPPER(TRIM(p.vesselName)),
    UPPER(TRIM(p.product.name)),
    UPPER(TRIM(p.dischargePort.displayName))
""")
    List<VesselStockGroupAggregate> sumPhysicalReadyByGroup();

    @Query("""
SELECT new chemos.chem_os.dto.VesselStockGroupAggregate(
    UPPER(TRIM(p.vesselName)),
    UPPER(TRIM(p.product.name)),
    UPPER(TRIM(p.dischargePort.displayName)),
    COALESCE(SUM(p.quantity), 0)
)
FROM Purchase p
WHERE LOWER(TRIM(p.marketStatus)) = 'ready'
  AND p.status.id = 'CONFIRMED'
  AND CAST(p.confirmedAt AS date) = :onDate
  AND p.isDeleted = false
GROUP BY
    UPPER(TRIM(p.vesselName)),
    UPPER(TRIM(p.product.name)),
    UPPER(TRIM(p.dischargePort.displayName))
""")
    List<VesselStockGroupAggregate> sumPhysicalReadyByGroup(@Param("onDate") LocalDate onDate);

<<<<<<< Updated upstream
    @Query("""
        SELECT new chemos.chem_os.dto.VesselStockGroupAggregate(
            UPPER(TRIM(p.vesselName)),
            UPPER(TRIM(p.product.name)),
            UPPER(TRIM(p.dischargePort.displayName)),
            COALESCE(SUM(p.quantity), 0)
        )
        FROM Purchase p
        WHERE LOWER(TRIM(p.marketStatus)) = 'ready'
          AND p.status.id = 'CONFIRMED'
          AND p.confirmedAt > :after
        GROUP BY
            UPPER(TRIM(p.vesselName)),
            UPPER(TRIM(p.product.name)),
            UPPER(TRIM(p.dischargePort.displayName))
        """)
    List<VesselStockGroupAggregate> sumPhysicalReadyAfter(@Param("after") LocalDateTime after);
=======
    Optional<Purchase> findByIdAndIsDeletedFalse(String id);

    List<Purchase> findByStatus_IdAndIsDeletedFalse(String statusId); //status-confirm
    List<Purchase> findByIdInAndIsDeletedFalse(List<String> ids);
    boolean existsByIdAndIsDeletedFalse(String id); //import
>>>>>>> Stashed changes
}