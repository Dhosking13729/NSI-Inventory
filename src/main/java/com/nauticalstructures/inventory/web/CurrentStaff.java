package com.nauticalstructures.inventory.web;

import com.nauticalstructures.inventory.service.StaffUserDetails;
import org.springframework.security.core.Authentication;

final class CurrentStaff {
    private CurrentStaff() { }

    static Integer id(Authentication auth) {
        return ((StaffUserDetails) auth.getPrincipal()).getStaffId();
    }
}
