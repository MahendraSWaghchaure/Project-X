package com.projectx.app.bootstrap;

import com.projectx.common.security.Role;
import com.projectx.identity.repo.UserAccountRepository;
import com.projectx.identity.service.UserProvisioningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Ensures there's always at least one PLATFORM_ADMIN account to log in with, so a fresh environment isn't a chicken-and-egg problem. */
@Component
public class PlatformAdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformAdminSeeder.class);

    private final UserAccountRepository userAccountRepository;
    private final UserProvisioningService userProvisioningService;
    private final String adminEmail;
    private final String adminPassword;

    public PlatformAdminSeeder(UserAccountRepository userAccountRepository, UserProvisioningService userProvisioningService,
                                @Value("${projectx.bootstrap.admin-email:admin@projectx.local}") String adminEmail,
                                @Value("${projectx.bootstrap.admin-password:changeme123}") String adminPassword) {
        this.userAccountRepository = userAccountRepository;
        this.userProvisioningService = userProvisioningService;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userAccountRepository.existsByEmailIgnoreCase(adminEmail)) {
            return;
        }
        userProvisioningService.provision(adminEmail, null, adminPassword, Role.PLATFORM_ADMIN, null);
        log.warn("Seeded PLATFORM_ADMIN {} - change projectx.bootstrap.admin-password (or its default) before going anywhere near production", adminEmail);
    }
}
