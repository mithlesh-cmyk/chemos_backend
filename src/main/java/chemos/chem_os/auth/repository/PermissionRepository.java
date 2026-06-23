package chemos.chem_os.auth.repository;

import chemos.chem_os.auth.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, String> {

    Optional<Permission> findByPermissionCode(String permissionCode);
}
