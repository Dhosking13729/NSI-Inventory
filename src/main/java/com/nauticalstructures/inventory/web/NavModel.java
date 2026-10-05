package com.nauticalstructures.inventory.web;

import com.nauticalstructures.inventory.service.AlertService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** In-app notification: Purchasing and Admin see the number of Open alerts in the top bar on every page. */
@ControllerAdvice
public class NavModel {

    private final AlertService alerts;

    public NavModel(AlertService alerts) { this.alerts = alerts; }

    @ModelAttribute("openAlertCount")
    public Long openAlertCount(Authentication auth) {
        if (auth == null || auth.getAuthorities().stream().noneMatch(a ->
                a.getAuthority().equals("ROLE_PURCHASING") || a.getAuthority().equals("ROLE_ADMIN"))) {
            return null;
        }
        return alerts.openCount();
    }
}
