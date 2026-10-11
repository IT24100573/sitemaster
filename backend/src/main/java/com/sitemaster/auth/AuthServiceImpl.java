package com.sitemaster.auth;

import com.sitemaster.auth.dto.ForgotPasswordRequest;
import com.sitemaster.auth.dto.LoginRequest;
import com.sitemaster.auth.dto.ResetPasswordRequest;
import com.sitemaster.common.config.AppProperties;
import com.sitemaster.common.exception.AuthenticationFailedException;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.security.PrincipalFactory;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.security.TokenHasher;
import com.sitemaster.user.User;
import com.sitemaster.user.UserRepository;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * Implementation of {@link AuthService}.
 * <ul>
 *   <li>Same "Invalid email or password" message for unknown email and wrong password,
 *       and a dummy BCrypt check for unknown emails so timing does not reveal either.</li>
 *   <li>Locks the account after the configured number of failures for the configured
 *       duration (5 / 15 minutes by default).</li>
 *   <li>Disabled accounts get a clear message, but only after the password is correct.</li>
 *   <li>Reset tokens are random, stored hashed, expire after 30 minutes and work once.</li>
 * </ul>
 * OOP concepts: Abstraction (implements AuthService) and Encapsulation (lock and
 * token rules are delegated to the User and PasswordResetToken entities).
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String RESET_PATH = "/reset-password?token=";

    private final UserRepository userRepository;
    private final PrincipalFactory principalFactory;
    private final WorkflowEventPublisher eventPublisher;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;
    private final Clock clock;
    private final String dummyPasswordHash;

    public AuthServiceImpl(UserRepository userRepository,
                           PrincipalFactory principalFactory,
                           WorkflowEventPublisher eventPublisher,
                           PasswordResetTokenRepository tokenRepository,
                           PasswordEncoder passwordEncoder,
                           AppProperties properties,
                           Clock clock) {
        this.userRepository = userRepository;
        this.principalFactory = principalFactory;
        this.eventPublisher = eventPublisher;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode(TokenHasher.newToken());
    }

    @Override
    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public SiteMasterUserPrincipal authenticate(LoginRequest request) {
        Optional<User> found = userRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail()));
        if (found.isEmpty()) {
            passwordEncoder.matches(request.getPassword(), dummyPasswordHash);
            throw AuthenticationFailedException.invalidCredentials();
        }

        User user = found.get();
        LocalDateTime now = LocalDateTime.now(clock);

        if (user.isLockedAt(now)) {
            throw AuthenticationFailedException.accountLocked(minutesUntil(user.getLockedUntil(), now));
        }
        user.clearExpiredLock(now);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            AppProperties.Security security = properties.security();
            user.registerFailedLogin(security.maxFailedAttempts(), security.lockDuration(), now);
            userRepository.save(user);
            log.info("Failed sign-in for user id {} ({} consecutive)", user.getId(), user.getFailedLoginAttempts());
            throw AuthenticationFailedException.invalidCredentials();
        }

        if (!user.isActive()) {
            throw AuthenticationFailedException.accountDisabled();
        }

        user.registerSuccessfulLogin(now);
        userRepository.save(user);
        eventPublisher.publish(WorkflowEvent.builder(WorkflowEventType.USER_LOGGED_IN)
                .actor(user.getId(), user.getFullName())
                .entityId(user.getId())
                .build());
        return principalFactory.create(user);
    }

    @Override
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail()))
                .filter(User::isActive)
                .ifPresent(user -> issuePasswordResetLink(user.getId()));
    }

    @Override
    @Transactional
    public String issuePasswordResetLink(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "User"));
        LocalDateTime now = LocalDateTime.now(clock);
        tokenRepository.invalidateActiveTokens(user.getId(), now);

        String rawToken = TokenHasher.newToken();
        LocalDateTime expiresAt = now.plus(properties.security().resetTokenExpiry());
        tokenRepository.save(new PasswordResetToken(user, TokenHasher.sha256(rawToken), expiresAt));

        // On localhost there is no mail server: the link is written to the console.
        log.info("Password link for {} (valid until {}): {}{}{}",
                user.getEmail(), expiresAt, properties.frontendUrl(), RESET_PATH, rawToken);
        return rawToken;
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = tokenRepository.findByTokenHash(TokenHasher.sha256(request.getToken()))
                .orElseThrow(() -> new BusinessRuleException("auth.resetToken.invalid"));
        LocalDateTime now = LocalDateTime.now(clock);
        if (token.isUsed()) {
            throw new BusinessRuleException("auth.resetToken.invalid");
        }
        if (token.isExpiredAt(now)) {
            throw new BusinessRuleException("auth.resetToken.expired");
        }

        User user = token.getUser();
        user.changePasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.unlock();
        token.markUsed(now);
        userRepository.save(user);
        tokenRepository.save(token);
        eventPublisher.publish(WorkflowEvent.builder(WorkflowEventType.PASSWORD_CHANGED)
                .actor(user.getId(), user.getFullName())
                .entityId(user.getId())
                .recipients(user.getId())
                .link("profile")
                .build());
        log.info("Password reset completed for user id {}", user.getId());
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static long minutesUntil(LocalDateTime until, LocalDateTime now) {
        long seconds = Duration.between(now, until).getSeconds();
        return Math.max(1, (seconds + 59) / 60);
    }
}
