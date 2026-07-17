package chemos.chem_os.repository;

import chemos.chem_os.model.Countries;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CountryRepository extends JpaRepository<Countries, String> {

    Optional<Countries> findByDisplayNameIgnoreCase(String displayName);

    @Query(value = """
    SELECT *
    FROM countries
    WHERE
        :query = ''
        OR search_key LIKE LOWER(CONCAT('%', :query, '%'))
        OR similarity(search_key, LOWER(:query)) > 0.25
    ORDER BY
        CASE
            WHEN :query = '' THEN 1
            WHEN search_key = LOWER(:query) THEN 1
            WHEN search_key LIKE LOWER(CONCAT(:query, '%')) THEN 2
            WHEN search_key LIKE LOWER(CONCAT('% ', :query, '%')) THEN 3
            WHEN search_key LIKE LOWER(CONCAT('%(', :query, '%')) THEN 4
            WHEN search_key LIKE LOWER(CONCAT('%', :query, '%')) THEN 5
            ELSE 6
        END,
        similarity(search_key, LOWER(:query)) DESC,
        display_name ASC
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM countries
    WHERE
        :query = ''
        OR search_key LIKE LOWER(CONCAT('%', :query, '%'))
        OR similarity(search_key, LOWER(:query)) > 0.25
    """,
            nativeQuery = true)
    Page<Countries> searchCountries(@Param("query") String query, Pageable pageable);
}