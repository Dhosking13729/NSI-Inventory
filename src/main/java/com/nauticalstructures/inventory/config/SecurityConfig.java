package com.nauticalstructures.inventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Every use case starts with Log In. Role rules follow the Submission 3 use case diagram:
 * Stockroom staff check materials in and out; Admin runs the spreadsheet import and manages staff users;
 * Purchasing works the alerts and reorder requests and sets reorder thresholds (Version 2). Admin can do everything.
 */
@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/login", "/error", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/scan/**").hasAnyRole("STOCKROOM", "ADMIN")
                        .requestMatchers("/import/**", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/alerts/**", "/reorders/**").hasAnyRole("PURCHASING", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/materials/*/threshold").hasAnyRole("PURCHASING", "ADMIN")
                        .requestMatchers("/materials/new", "/materials/*/edit").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/materials/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
                .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; style-src 'self'; script-src 'self'")));
        return http.build();
    }
}
