package chemos.chem_os.repository;

import chemos.chem_os.model.IncomingUnsoldSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface IncomingUnsoldSnapshotRepository extends JpaRepository<IncomingUnsoldSnapshot, String> {

    Optional<IncomingUnsoldSnapshot> findBySnapshotDateAndVesselNameAndProductAndPort(
            LocalDate snapshotDate, String vesselName, String product, String port);

    Optional<IncomingUnsoldSnapshot> findTopByVesselNameAndProductAndPortAndSnapshotDateLessThanOrderBySnapshotDateDesc(
            String vesselName, String product, String port, LocalDate beforeDate);
}
