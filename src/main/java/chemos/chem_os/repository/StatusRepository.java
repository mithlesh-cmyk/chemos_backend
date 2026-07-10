package chemos.chem_os.repository;

import chemos.chem_os.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatusRepository extends JpaRepository<Status, String> {
    Optional<Status> findByNameIgnoreCase(String name);
}
