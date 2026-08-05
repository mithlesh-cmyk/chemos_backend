package chemos.chem_os.repository;

import chemos.chem_os.model.RevenueCsvEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface RevenueCsvEntryRepository extends JpaRepository<RevenueCsvEntry, Long> {
    List<RevenueCsvEntry> findByUploadId(Long uploadId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM RevenueCsvEntry e")
    BigDecimal sumAllAmounts();
}
