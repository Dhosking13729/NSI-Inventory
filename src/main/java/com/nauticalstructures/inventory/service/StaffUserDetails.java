package com.nauticalstructures.inventory.service;

import com.nauticalstructures.inventory.domain.StaffUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** The logged-in staff member. Carries StaffID so every scan is recorded against the person who made it. */
public class StaffUserDetails implements UserDetails {
    private final Integer staffId;
    private final String name;
    private final String username;
    private final String passwordHash;
    private final String roleLabel;
    private final List<GrantedAuthority> authorities;

    public StaffUserDetails(StaffUser user) {
        this.staffId = user.getStaffId();
        this.name = user.getName();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.roleLabel = user.getRole().label();
        this.authorities = List.of(new SimpleGrantedAuthority(user.getRole().authority()));
    }

    public Integer getStaffId() { return staffId; }
    public String getName() { return name; }
    public String getRoleLabel() { return roleLabel; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return username; }
}
