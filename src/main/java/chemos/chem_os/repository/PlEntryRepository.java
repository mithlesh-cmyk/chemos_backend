package chemos.chem_os.repository;

import chemos.chem_os.model.PlEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlEntryRepository extends JpaRepository<PlEntry, Long> {
    List<PlEntry> findByUploadId(Long uploadId);
}
