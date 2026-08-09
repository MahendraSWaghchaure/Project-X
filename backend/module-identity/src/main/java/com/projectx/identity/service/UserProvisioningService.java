package com.projectx.identity.service;

import com.projectx.common.exception.ConflictException;
import com.projectx.common.security.Role;
import com.projectx.identity.domain.UserAccount;
import com.projectx.identity.domain.UserStatus;
import com.projectx.identity.repo.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The one place a login identity gets created. module-school (school admins) and module-people
 * (drivers/parents) call this when onboarding someone instead of touching UserAccount directly,
 * keeping password hashing and email-uniqueness rules in a single spot.
 */
@Service
@Transactional
public class UserProvisioningService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProvisioningService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserAccount provision(String email, String phone, String rawPassword, Role role, Long schoolId) {
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already registered: " + email);
        }
        UserAccount user = new UserAccount(email, phone, passwordEncoder.encode(rawPassword), role, schoolId);
        return userAccountRepository.save(user);
    }

    /** For invite-based onboarding (parents): account exists but can't log in until they accept and set a password. */
    public UserAccount provisionInvited(String email, String phone, Role role, Long schoolId) {
        UserAccount user = provision(email, phone, passwordEncoder.encode(TemporaryPassword.random()), role, schoolId);
        user.setStatus(UserStatus.INVITED);
        return user;
    }

    private static final class TemporaryPassword {
        static String random() {
            return java.util.UUID.randomUUID().toString();
        }
    }
}
