package chemos.chem_os.auth.repository;

import chemos.chem_os.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String s);

    // Loads user + role + role's direct permissions + parent role + parent's permissions
    // in a single query — avoids lazy-load exceptions in JwtAuthFilter permission resolution.
    @Query("""
            SELECT u FROM User u
            JOIN FETCH u.role r
            LEFT JOIN FETCH r.permissions
            LEFT JOIN FETCH r.parentRole pr
            LEFT JOIN FETCH pr.permissions
            WHERE u.username = :username
            """)
    Optional<User> findByUsernameWithPermissions(@Param("username") String username);
}
