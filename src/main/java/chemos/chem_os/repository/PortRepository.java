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
                OR word_similarity(search_key, LOWER(:query)) >= 0.20
                OR similarity(search_key, LOWER(:query)) >= 0.20
            )
        ORDER BY
            CASE
                -- Empty search
                WHEN :query = '' THEN 1

                -- Exact LOCODE
                WHEN LOWER(COALESCE(locode, '')) = LOWER(:query) THEN 1

                -- Exact port name
                WHEN search_key = LOWER(:query) THEN 1

                -- Port starts with query
                WHEN search_key LIKE LOWER(CONCAT(:query, '%')) THEN 2

                -- LOCODE starts with query
                WHEN LOWER(COALESCE(locode, '')) LIKE LOWER(CONCAT(:query, '%')) THEN 3

                -- Any word starts with query
                WHEN search_key LIKE LOWER(CONCAT('% ', :query, '%')) THEN 4

                -- Text inside brackets
                WHEN search_key LIKE LOWER(CONCAT('%(', :query, '%')) THEN 5

                -- Contains query
                WHEN search_key LIKE LOWER(CONCAT('%', :query, '%')) THEN 6

                -- Fuzzy matches
                ELSE 7
            END,
            LENGTH(search_key) ASC,
            GREATEST(
                word_similarity(search_key, LOWER(:query)),
                similarity(search_key, LOWER(:query))
            ) DESC,
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
                OR word_similarity(search_key, LOWER(:query)) >= 0.20
                OR similarity(search_key, LOWER(:query)) >= 0.20
            )
        """,
            nativeQuery = true)
    Page<Ports> searchPorts(
            @Param("query") String query,
            @Param("isIndian") Boolean isIndian,
            Pageable pageable
    );

    boolean existsBySearchKey(String searchKey);

    java.util.Optional<Ports> findByDisplayNameIgnoreCase(String displayName);
}