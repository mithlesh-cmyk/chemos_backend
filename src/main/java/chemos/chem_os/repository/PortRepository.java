package chemos.chem_os.repository;

import chemos.chem_os.model.Ports;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PortRepository extends JpaRepository<Ports, String> {

    @Query(value = """
    SELECT *
    FROM ports
    WHERE
        (:isIndian IS NULL OR is_indian = :isIndian)
        AND (
            :query = ''
            OR search_key LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(COALESCE(locode, '')) LIKE LOWER(CONCAT('%', :query, '%'))
        )
    ORDER BY
        CASE
            WHEN :query = '' THEN 1
            WHEN search_key LIKE LOWER(CONCAT(:query, '%')) THEN 1
            WHEN LOWER(COALESCE(locode, '')) LIKE LOWER(CONCAT(:query, '%')) THEN 2
            ELSE 3
        END,
        display_name ASC
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM ports
    WHERE
        (:isIndian IS NULL OR is_indian = :isIndian)
        AND (
            :query = ''
            OR search_key LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(COALESCE(locode, '')) LIKE LOWER(CONCAT('%', :query, '%'))
        )
    """,
            nativeQuery = true)
    Page<Ports> searchPorts(@Param("query") String query, @Param("isIndian") Boolean isIndian, Pageable pageable);

    boolean existsBySearchKey(String searchKey);
}
