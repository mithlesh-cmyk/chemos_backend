package chemos.chem_os.repository;

import chemos.chem_os.model.Products;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Products, String> {

    Optional<Products> findByNameIgnoreCase(String name);

    @Query(value = """
    SELECT *
    FROM products p
    WHERE p.is_active = true
      AND (
          -- If query is empty, return everything. Otherwise, evaluate search conditions.
          :query = ''
          OR similarity(LOWER(p.name), LOWER(:query)) > 0.2
          OR LOWER(p.hs_code) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(p.cas_no) LIKE LOWER(CONCAT('%', :query, '%'))
      )
    ORDER BY
        -- 1. Handle ordering when there is no query
        CASE
            WHEN :query = '' THEN 1
            WHEN LOWER(p.name) = LOWER(:query) THEN 1
            WHEN LOWER(p.name) LIKE LOWER(CONCAT(:query, '%')) THEN 2
            ELSE 3
        END,
        -- 2. Similarity score ordering (only relevant when query is present)
        CASE
            WHEN :query = '' THEN 0
            ELSE similarity(LOWER(p.name), LOWER(:query))
        END DESC,
        -- 3. Alphabetical fallback
        p.name ASC
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM products p
    WHERE p.is_active = true
      AND (
          :query = ''
          OR similarity(LOWER(p.name), LOWER(:query)) > 0.2
          OR LOWER(p.hs_code) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(p.cas_no) LIKE LOWER(CONCAT('%', :query, '%'))
      )
    """,
            nativeQuery = true)
    Page<Products> searchProducts(@Param("query") String query, Pageable pageable);
}