package chemos.chem_os.repository;

import chemos.chem_os.model.Companies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface CompanyRepository extends JpaRepository<Companies, String> {

    Optional<Companies> findBySearchKey(String searchKey);

    Optional<Companies> findByDisplayNameIgnoreCase(String displayName);

    @Query(
            value = """
    SELECT *
    FROM companies
    WHERE
        :prefix = ''
        OR search_key LIKE CONCAT('%', :prefix, '%')
        OR word_similarity(search_key, :prefix) >= 0.20
        OR similarity(search_key, :prefix) >= 0.20
    ORDER BY
        CASE
            -- 1. Exact match
            WHEN search_key = :prefix THEN 1

            -- 2. Starts with query
            WHEN search_key LIKE CONCAT(:prefix, '%') THEN 2

            -- 3. Any word starts with query
            WHEN search_key LIKE CONCAT('% ', :prefix, '%') THEN 3

            -- 4. Acronym in brackets
            WHEN search_key LIKE CONCAT('%(', :prefix, '%') THEN 4

            -- 5. Contains query
            WHEN search_key LIKE CONCAT('%', :prefix, '%') THEN 5

            -- 6. Fuzzy match
            ELSE 6
        END,

        LENGTH(search_key) ASC,

        GREATEST(
            word_similarity(search_key, :prefix),
            similarity(search_key, :prefix)
        ) DESC,

        display_name ASC

    LIMIT :limit
    """,
            nativeQuery = true
    )
    List<Companies> findSuggestions(
            @Param("prefix") String prefix,
            @Param("limit") int limit
    );
}