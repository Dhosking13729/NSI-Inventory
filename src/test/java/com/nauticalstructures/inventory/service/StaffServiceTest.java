package com.nauticalstructures.inventory.service;

import com.nauticalstructures.inventory.domain.StaffRole;
import com.nauticalstructures.inventory.domain.StaffUser;
import com.nauticalstructures.inventory.repository.StaffUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class StaffServiceTest {

    @Autowired StaffService service;
    @Autowired StaffUserRepository repo;
    @Autowired PasswordEncoder encoder;

    @Test
    void passwordIsStoredAsASaltedHashNeverPlaintext() {
        StaffUser u = service.create("Pat Purchasing", StaffRole.PURCHASING, "Pat.P", "a-long-password-1");
        assertThat(u.getUsername()).isEqualTo("pat.p");
        assertThat(u.getPasswordHash()).isNotEqualTo("a-long-password-1").startsWith("$2");
        assertThat(encoder.matches("a-long-password-1", u.getPasswordHash())).isTrue();
        StaffUser twin = service.create("Pat Twin", StaffRole.PURCHASING, "pat.twin", "a-long-password-1");
        assertThat(twin.getPasswordHash()).isNotEqualTo(u.getPasswordHash());
    }

    @Test
    void loginLookupCarriesStaffIdAndRole() {
        StaffUser u = service.create("Sam Stockroom", StaffRole.STOCKROOM, "sam.s", "a-long-password-1");
        StaffUserDetails d = (StaffUserDetails) service.loadUserByUsername(" SAM.S ");
        assertThat(d.getStaffId()).isEqualTo(u.getStaffId());
        assertThat(d.getAuthorities()).extracting("authority").containsExactly("ROLE_STOCKROOM");
        assertThat(d.getRoleLabel()).isEqualTo("Stockroom");
        assertThatThrownBy(() -> service.loadUserByUsername("nobody")).isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void rejectsDuplicateUsernamesWeakPasswordsAndBadInput() {
        service.create("A Person", StaffRole.ADMIN, "dup.user", "a-long-password-1");
        assertThatThrownBy(() -> service.create("B", StaffRole.ADMIN, "DUP.user", "a-long-password-2")).hasMessageContaining("already taken");
        assertThatThrownBy(() -> service.create("C", StaffRole.ADMIN, "c.user", "short")).hasMessageContaining("at least 10");
        assertThatThrownBy(() -> service.create("D", StaffRole.ADMIN, "has space", "a-long-password-1")).hasMessageContaining("Username");
        assertThatThrownBy(() -> service.create(" ", StaffRole.ADMIN, "e.user", "a-long-password-1")).hasMessageContaining("Name");
        assertThatThrownBy(() -> service.create("F", null, "f.user", "a-long-password-1")).hasMessageContaining("Role");
    }

    @Test
    void firstStartCreatesTheAdminOnlyWhenNoStaffExist() {
        assertThat(repo.count()).isZero();
        new StaffService(repo, encoder, "first-admin-password").bootstrapAdmin();
        assertThat(repo.findByUsernameIgnoreCase("admin")).get()
                .satisfies(a -> assertThat(a.getRole()).isEqualTo(StaffRole.ADMIN));
        new StaffService(repo, encoder, "first-admin-password").bootstrapAdmin();
        assertThat(repo.count()).isEqualTo(1);
        new StaffService(repo, encoder, "").bootstrapAdmin();
        assertThat(repo.count()).isEqualTo(1);
    }
}
