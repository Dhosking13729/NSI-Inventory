package com.nauticalstructures.inventory.repository;

import com.nauticalstructures.inventory.domain.StaffRole;
import com.nauticalstructures.inventory.domain.StaffUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffUserRepository extends JpaRepository<StaffUser, Integer> {
    Optional<StaffUser> findByUsernameIgnoreCase(String username);
    List<StaffUser> findAllByOrderByNameAsc();

    Optional<StaffUser> findFirstByRoleOrderByStaffIdAsc(StaffRole role);
}
