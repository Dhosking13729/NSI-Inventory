package com.nauticalstructures.inventory.service;

import com.nauticalstructures.inventory.domain.StaffRole;
import com.nauticalstructures.inventory.domain.StaffUser;
import com.nauticalstructures.inventory.repository.StaffUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Log In and Manage Staff Users use cases. */
@Service
@Transactional
public class StaffService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(StaffService.class);
    public static final int MIN_PASSWORD_LENGTH = 10;

    private final StaffUserRepository staff;
    private final PasswordEncoder encoder;
    private final String bootstrapAdminPassword;

    public StaffService(StaffUserRepository staff, PasswordEncoder encoder,
                        @Value("${nsi.bootstrap-admin-password:}") String bootstrapAdminPassword) {
        this.staff = staff;
        this.encoder = encoder;
        this.bootstrapAdminPassword = bootstrapAdminPassword;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return staff.findByUsernameIgnoreCase(username.trim()).map(StaffUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }

    @Transactional(readOnly = true)
    public List<StaffUser> list() { return staff.findAllByOrderByNameAsc(); }

    @Transactional(readOnly = true)
    public StaffUser get(Integer staffId) {
        return staff.findById(staffId).orElseThrow(() -> new NotFoundException("No staff user #" + staffId));
    }

    public StaffUser create(String name, StaffRole role, String username, String rawPassword) {
        if (name == null || name.isBlank()) throw new BusinessRuleException("Name is required");
        if (role == null) throw new BusinessRuleException("Role is required");
        if (username == null || !username.trim().matches("[A-Za-z0-9._-]{3,50}")) {
            throw new BusinessRuleException("Username must be 3-50 letters, digits, '.', '_' or '-'");
        }
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessRuleException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (staff.findByUsernameIgnoreCase(username.trim()).isPresent()) {
            throw new BusinessRuleException("Username " + username.trim() + " is already taken");
        }
        return staff.save(new StaffUser(name.trim(), role, username.trim().toLowerCase(), encoder.encode(rawPassword)));
    }

    /** First start in an empty environment: create the 'admin' account from NSI_BOOTSTRAP_ADMIN_PASSWORD, if set. */
    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapAdmin() {
        if (staff.count() == 0 && !bootstrapAdminPassword.isBlank()) {
            create("Inventory Manager", StaffRole.ADMIN, "admin", bootstrapAdminPassword);
            log.info("Created initial Admin account 'admin' - change its password after first login");
        }
    }
}
