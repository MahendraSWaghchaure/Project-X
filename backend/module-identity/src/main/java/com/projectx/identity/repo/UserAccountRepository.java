package com.projectx.identity.repo;

import com.projectx.common.security.Role;
import com.projectx.identity.domain.UserAccount;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** School staff of one role (teachers, admins) - always paged/capped by the caller. */
    Page<UserAccount> findBySchoolIdAndRole(Long schoolId, Role role, Pageable pageable);

    /** Platform-wide holders of a role (e.g. the platform admins an inquiry is routed to) - always paged/capped by the caller. */
    Page<UserAccount> findByRole(Role role, Pageable pageable);
}
