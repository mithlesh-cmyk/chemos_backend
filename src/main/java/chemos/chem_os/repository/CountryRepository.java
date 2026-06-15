package chemos.chem_os.repository;

import chemos.chem_os.model.Countries;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CountryRepository extends JpaRepository<Countries, String> {

    @Query(value = """
    SELECT *
    FROM countries
    WHERE
        -- Short-circuit: If query is blank, skip filter validation and grab everything
        :query = ''
        OR search_key LIKE LOWER(CONCAT('%', :query, '%'))
    ORDER BY
        CASE
            -- If query is empty, treat all rows with equal relevance priority
            WHEN :query = '' THEN 1
            -- If user typed something, prioritize prefix matches
            WHEN search_key LIKE LOWER(CONCAT(:query, '%')) THEN 1
            ELSE 2
        END,
        display_name ASC
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM countries
    WHERE :query = ''
       OR search_key LIKE LOWER(CONCAT('%', :query, '%'))
    """,
            nativeQuery = true)
    Page<Countries> searchCountries(@Param("query") String query, Pageable pageable);
}


