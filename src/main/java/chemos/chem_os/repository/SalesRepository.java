package chemos.chem_os.repository;

import chemos.chem_os.model.Sales;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface SalesRepository extends JpaRepository<Sales, String>, JpaSpecificationExecutor<Sales> {

    @Query("SELECT s FROM Sales s WHERE " +
            "(:product IS NULL OR s.product = :product) AND " +
            "(:companyTo IS NULL OR s.companyTo = :companyTo) AND " +
            "(:port IS NULL OR s.port = :port) AND " +
            "s.date >= :startDate AND s.date <= :endDate")
    Page<Sales> findWithFilters(
            @Param("product") String product,
            @Param("companyTo") String companyTo,
            @Param("port") String port,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );
}