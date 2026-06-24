package chemos.chem_os.auth.repository;

import chemos.chem_os.auth.model.UserPermissionRestriction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserPermissionRestrictionRepository extends JpaRepository<UserPermissionRestriction, Long> {

    @Query("SELECT r FROM UserPermissionRestriction r JOIN FETCH r.permission WHERE r.user.id = :userId")
    List<UserPermissionRestriction> findByUserId(@Param("userId") UUID userId);
}
