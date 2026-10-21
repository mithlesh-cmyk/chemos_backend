package chemos.chem_os.auth.repository;

import chemos.chem_os.auth.model.BackupCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BackupCodeRepository extends JpaRepository<BackupCode, UUID> {

    List<BackupCode> findByUserIdAndUsedFalse(UUID userId);

    long countByUserIdAndUsedFalse(UUID userId);

    void deleteByUserId(UUID userId);
}
