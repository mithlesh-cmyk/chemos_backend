package chemos.chem_os.repository;

import chemos.chem_os.model.VesselDishPortSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VesselDishPortSummaryRepository extends JpaRepository<VesselDishPortSummary, Long> {

    List<VesselDishPortSummary> findByProductIdOrderByIdAsc(String productId);
}
