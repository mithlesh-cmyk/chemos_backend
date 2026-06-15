package chemos.chem_os.repository;

import chemos.chem_os.model.Companies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Companies, String> {

    Optional<Companies> findBySearchKey(String searchKey);

    @Query(
            value = """
        SELECT *
        FROM companies
        WHERE
            -- Shortcut: If query input string is empty, fetch everything instantly
            :prefix = '' 
            OR search_key LIKE CONCAT('%', :prefix, '%')
        ORDER BY 
            CASE 
                -- 1. Highest priority: Company name starts with user text
                WHEN search_key LIKE CONCAT(:prefix, '%') THEN 1
                -- 2. Lower priority: Text matches somewhere in the middle
                ELSE 2
            END,
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