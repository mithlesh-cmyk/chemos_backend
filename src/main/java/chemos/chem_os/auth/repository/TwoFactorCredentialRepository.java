package chemos.chem_os.auth.repository;

import chemos.chem_os.auth.model.TwoFactorCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TwoFactorCredentialRepository extends JpaRepository<TwoFactorCredential, UUID> {

    Optional<TwoFactorCredential> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
