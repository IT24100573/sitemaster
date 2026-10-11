package com.sitemaster.seed;

import com.sitemaster.common.config.AppProperties;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.role.Role;
import com.sitemaster.role.RoleRepository;
import com.sitemaster.user.User;
import com.sitemaster.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Creates demo data on first start:
 * <ol>
 *   <li>one sign-in account per role, when the users table is empty;</li>
 *   <li>a full demo company ({@link DemoDataSeeder}), when no project exists yet.</li>
 * </ol>
 * Each step runs in its own transaction. If the demo company cannot be created
 * the error is logged and the application still starts with the accounts.
 * Roles and the default permission matrix are reference data and are created by
 * Flyway (V2__seed_roles_and_permissions.sql), not here.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** One demo account per role. Names use letters and spaces only, so they pass validation. */
    private static final List<SeedAccount> ACCOUNTS = List.of(
            new SeedAccount(RoleName.ADMIN, "admin@sitemaster.lk", "Anjali Wickramasinghe"),
            new SeedAccount(RoleName.PROJECT_MANAGER, "pm@sitemaster.lk", "Nimal Perera"),
            new SeedAccount(RoleName.SITE_ENGINEER, "engineer@sitemaster.lk", "Kasun Silva"),
            new SeedAccount(RoleName.WORKER, "worker@sitemaster.lk", "Saman Kumara"),
            new SeedAccount(RoleName.HR_MANAGER, "hr@sitemaster.lk", "Dilani Fernando"),
            new SeedAccount(RoleName.ACCOUNTANT, "accountant@sitemaster.lk", "Ruwan Jayasinghe"),
            new SeedAccount(RoleName.SUPPLIER, "supplier@sitemaster.lk", "Chaminda Rajapaksha"));

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;
    private final DemoDataSeeder demoDataSeeder;
    private final TransactionTemplate transactionTemplate;

    public DataSeeder(UserRepository userRepository, RoleRepository roleRepository,
                      PasswordEncoder passwordEncoder, AppProperties properties, DemoDataSeeder demoDataSeeder,
                      PlatformTransactionManager transactionManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.demoDataSeeder = demoDataSeeder;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        if (!properties.seed().enabled()) {
            log.info("Demo data seeding is disabled");
            return;
        }
        if (userRepository.count() == 0) {
            transactionTemplate.executeWithoutResult(status -> seedUsers());
        } else {
            log.info("Users already exist; demo accounts not created");
        }
        if (!demoDataSeeder.shouldRun()) {
            log.info("Projects already exist; demo company not created");
            return;
        }
        try {
            demoDataSeeder.seed();
        } catch (RuntimeException ex) {
            log.error("Demo company could not be created; the application continues with the demo accounts only", ex);
        }
    }

    private void seedUsers() {
        String passwordHash = passwordEncoder.encode(properties.seed().defaultPassword());
        for (SeedAccount account : ACCOUNTS) {
            Role role = roleRepository.findByName(account.role())
                    .orElseThrow(() -> new IllegalStateException(
                            "Role " + account.role() + " is missing; check the Flyway migrations"));
            userRepository.save(new User(account.fullName(), account.email(), passwordHash, role));
        }
        log.info("Seeded {} demo accounts (one per role)", ACCOUNTS.size());
    }

    private record SeedAccount(RoleName role, String email, String fullName) {
    }
}
