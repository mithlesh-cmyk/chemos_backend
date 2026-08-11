package chemos.chem_os.repository;

import chemos.chem_os.model.CostCsvEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CostCsvEntryRepository extends JpaRepository<CostCsvEntry, Long> {
    List<CostCsvEntry> findByUploadId(Long uploadId);

    @Query("SELECT COALESCE(SUM(e.directCost), 0) FROM CostCsvEntry e WHERE e.upload.id = :uploadId")
    BigDecimal sumDirectCostByUploadId(@Param("uploadId") Long uploadId);

    @Query("SELECT COALESCE(SUM(e.indirectCost), 0) FROM CostCsvEntry e WHERE e.upload.id = :uploadId")
    BigDecimal sumIndirectCostByUploadId(@Param("uploadId") Long uploadId);
}
