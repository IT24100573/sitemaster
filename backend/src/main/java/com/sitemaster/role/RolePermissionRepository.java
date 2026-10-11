package com.sitemaster.role;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Data access for the role-permission matrix. */
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    @Query("select rp.permission from RolePermission rp where rp.role.id = :roleId")
    List<Permission> findPermissionsByRoleId(@Param("roleId") Long roleId);

    @Query("select rp.role.id, rp.permission.id from RolePermission rp")
    List<Object[]> findAllRolePermissionPairs();

    @Modifying
    @Query("delete from RolePermission rp where rp.role.id = :roleId")
    int deleteByRoleId(@Param("roleId") Long roleId);
}
