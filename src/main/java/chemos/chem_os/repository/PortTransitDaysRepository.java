package chemos.chem_os.repository;

import chemos.chem_os.model.PortTransitDays;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortTransitDaysRepository extends JpaRepository<PortTransitDays, String> {

    Optional<PortTransitDays> findByFromPortIdAndToPortId(String fromPortId, String toPortId);

    List<PortTransitDays> findByFromPortId(String fromPortId);

    List<PortTransitDays> findByToPortId(String toPortId);

    boolean existsByFromPortIdAndToPortId(String fromPortId, String toPortId);
}
