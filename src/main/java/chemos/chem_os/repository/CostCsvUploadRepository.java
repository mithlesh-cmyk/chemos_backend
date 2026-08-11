package chemos.chem_os.repository;

import chemos.chem_os.model.CostCsvUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CostCsvUploadRepository extends JpaRepository<CostCsvUpload, Long> {
    Optional<CostCsvUpload> findTopByOrderByUploadedAtDesc();
}
