package chemos.chem_os.repository;

import chemos.chem_os.model.MarketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketStatusRepository extends JpaRepository<MarketStatus, String> {
}