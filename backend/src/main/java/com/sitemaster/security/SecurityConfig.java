package com.sitemaster.security;

import com.sitemaster.common.enums.RoleName;
import com.sitemaster.user.UserRepository;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Security rules for the REST API.
 * <ul>
 *   <li>Session-based authentication: POST /api/auth/login stores the user in an
 *       HttpOnly session cookie (timeout configured in application.yml).</li>
 *   <li>CSRF protection with an XSRF-TOKEN cookie that Axios echoes back in the
 *       X-XSRF-TOKEN header.</li>
 *   <li>URL rules per role path, plus method-level @PreAuthorize with
 *       MODULE:ACTION authorities. Ownership and project-membership checks live
 *       in the services.</li>
 *   <li>SessionRefreshFilter ends sessions of deactivated users and reloads
 *       permissions after an admin changes them.</li>
 *   <li>Every failure is answered with the shared JSON error body.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;
    private final String sessionCookieName;

    public SecurityConfig(JsonAuthenticationEntryPoint authenticationEntryPoint,
                          JsonAccessDeniedHandler accessDeniedHandler,
                          @Value("${server.servlet.session.cookie.name}") String sessionCookieName) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.sessionCookieName = sessionCookieName;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SecurityContextRepository securityContextRepository,
                                                   UserRepository userRepository,
                                                   PrincipalFactory principalFactory,
                                                   ErrorResponseWriter errorResponseWriter) throws Exception {
        SessionRefreshFilter sessionRefreshFilter = new SessionRefreshFilter(
                userRepository, principalFactory, securityContextRepository, errorResponseWriter);
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login", "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                        .requestMatchers("/api/admin/**").hasRole(RoleName.ADMIN.name())
                        .requestMatchers("/api/pm/**").hasRole(RoleName.PROJECT_MANAGER.name())
                        .requestMatchers("/api/site/**").hasRole(RoleName.SITE_ENGINEER.name())
                        .requestMatchers("/api/worker/**").hasRole(RoleName.WORKER.name())
                        .requestMatchers("/api/hr/**").hasRole(RoleName.HR_MANAGER.name())
                        .requestMatchers("/api/accounts/**").hasRole(RoleName.ACCOUNTANT.name())
                        .requestMatchers("/api/supplier/**").hasRole(RoleName.SUPPLIER.name())
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies(sessionCookieName)
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .addFilterBefore(sessionRefreshFilter, AuthorizationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
