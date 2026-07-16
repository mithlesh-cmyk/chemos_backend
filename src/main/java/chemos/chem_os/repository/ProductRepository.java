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
                :query = ''

                OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))

                OR LOWER(p.hs_code) LIKE LOWER(CONCAT('%', :query, '%'))

                OR LOWER(p.cas_no) LIKE LOWER(CONCAT('%', :query, '%'))

                OR similarity(LOWER(p.name), LOWER(:query)) > 0.18
          )

        ORDER BY

            CASE

                -- Exact name
                WHEN LOWER(p.name) = LOWER(:query) THEN 1

                -- Exact HS Code
                WHEN LOWER(p.hs_code) = LOWER(:query) THEN 2

                -- Exact CAS No
                WHEN LOWER(p.cas_no) = LOWER(:query) THEN 3

                -- Starts with
                WHEN LOWER(p.name) LIKE LOWER(CONCAT(:query, '%')) THEN 4
                -- Word starts
                WHEN LOWER(p.name) LIKE LOWER(CONCAT('% ', :query, '%')) THEN 5

                -- Acronym (MEA, MX, OX...)
                WHEN LOWER(p.name) LIKE LOWER(CONCAT('%(', :query, ')%')) THEN 6

                -- Contains
                WHEN LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) THEN 7

                ELSE 8

            END,

            similarity(
                LOWER(p.name),
                LOWER(:query)
            ) DESC,

            LOWER(p.name) ASC
        """,

            countQuery = """
        SELECT COUNT(*)
        FROM products p
        WHERE p.is_active = true
          AND (
                :query = ''
                OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(p.hs_code) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(p.cas_no) LIKE LOWER(CONCAT('%', :query, '%'))
                OR similarity(LOWER(p.name), LOWER(:query)) > 0.18
          )
        """,

            nativeQuery = true)
    Page<Products> searchProducts(
            @Param("query") String query,
            Pageable pageable
    );
}