package chemos.chem_os.repository;

import chemos.chem_os.model.Salespersons;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SalespersonRepository extends JpaRepository<Salespersons, String> {
    @Query(value = """
    SELECT *
    FROM salespersons
    WHERE
        :query = ''
        OR LOWER(name) LIKE LOWER(CONCAT('%', :query, '%'))
        OR similarity(LOWER(name), LOWER(:query)) > 0.25
    ORDER BY
        CASE
            WHEN :query = '' THEN 1
            WHEN LOWER(name) = LOWER(:query) THEN 1
            WHEN LOWER(name) LIKE LOWER(CONCAT(:query, '%')) THEN 2
            WHEN LOWER(name) LIKE LOWER(CONCAT('% ', :query, '%')) THEN 3
            WHEN LOWER(name) LIKE LOWER(CONCAT('%', :query, '%')) THEN 4
            ELSE 5
        END,
        CASE
            WHEN :query = '' THEN 0
            ELSE similarity(LOWER(name), LOWER(:query))
        END DESC,
        name ASC
    LIMIT 20
    """, nativeQuery = true)
    List<Salespersons> searchSalespersons(@Param("query") String query);

    Optional<Salespersons> findByNameIgnoreCase(String name);
}