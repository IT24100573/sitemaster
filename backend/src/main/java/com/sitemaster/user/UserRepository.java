package com.sitemaster.user;

import com.sitemaster.common.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Data access for users. Email lookups are case-insensitive. */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("select new com.sitemaster.user.UserSecurityState(u.active, u.securityVersion) from User u where u.id = :id")
    Optional<UserSecurityState> findSecurityState(@Param("id") Long id);

    long countByRoleNameAndActiveTrue(RoleName roleName);

    long countByRoleId(Long roleId);

    @Query("select u.id from User u where u.role.name = :roleName and u.active = true")
    List<Long> findActiveIdsByRole(@Param("roleName") RoleName roleName);

    /** Makes every open session of a role reload its permissions. */
    @Modifying
    @Query("update User u set u.securityVersion = u.securityVersion + 1 where u.role.id = :roleId")
    int bumpSecurityVersionForRole(@Param("roleId") Long roleId);
}
