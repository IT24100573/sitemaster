package com.sitemaster.role;

import com.sitemaster.common.enums.AppModule;
import com.sitemaster.common.enums.PermissionAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Data access for permissions. */
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByModuleAndAction(AppModule module, PermissionAction action);
}
