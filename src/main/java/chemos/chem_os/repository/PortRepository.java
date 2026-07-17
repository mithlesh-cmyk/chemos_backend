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
            OR similarity(search_key, LOWER(:query)) > 0.2
        )
    ORDER BY
        CASE
            WHEN :query = '' THEN 1
            WHEN LOWER(COALESCE(locode, '')) = LOWER(:query) THEN 1
            WHEN search_key = LOWER(:query) THEN 1
            WHEN search_key LIKE LOWER(CONCAT(:query, '%')) THEN 2
            WHEN LOWER(COALESCE(locode, '')) LIKE LOWER(CONCAT(:query, '%')) THEN 3
            WHEN search_key LIKE LOWER(CONCAT('% ', :query, '%')) THEN 4
            WHEN search_key LIKE LOWER(CONCAT('%(', :query, '%')) THEN 5
            WHEN search_key LIKE LOWER(CONCAT('%', :query, '%')) THEN 6
            ELSE 7
        END,
        similarity(search_key, LOWER(:query)) DESC,
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
            OR similarity(search_key, LOWER(:query)) > 0.2
        )
    """,
            nativeQuery = true)
    Page<Ports> searchPorts(@Param("query") String query, @Param("isIndian") Boolean isIndian, Pageable pageable);

    boolean existsBySearchKey(String searchKey);

    java.util.Optional<Ports> findByDisplayNameIgnoreCase(String displayName);
}