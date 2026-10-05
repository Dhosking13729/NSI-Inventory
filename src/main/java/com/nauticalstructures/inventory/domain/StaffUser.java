package com.nauticalstructures.inventory.domain;

import jakarta.persistence.*;

/** Data dictionary table: StaffUser. PasswordHash holds a salted BCrypt hash; plaintext is never stored. */
@Entity
@Table(name = "staff_user")
public class StaffUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_id")
    private Integer staffId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private StaffRole role;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    protected StaffUser() { }

    public StaffUser(String name, StaffRole role, String username, String passwordHash) {
        this.name = name;
        this.role = role;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public Integer getStaffId() { return staffId; }
    public String getName() { return name; }
    public StaffRole getRole() { return role; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
}
