package chemos.chem_os.repository;

import chemos.chem_os.model.Salespersons;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SalespersonRepository extends JpaRepository<Salespersons, Long> {
    @Query(value = """
            SELECT *
            FROM salespersons
            WHERE :query = ''
               OR LOWER(name) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY name ASC
            """, nativeQuery = true)
    List<Salespersons> searchSalespersons(@Param("query") String query);
}