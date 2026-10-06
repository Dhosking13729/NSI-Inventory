package com.nauticalstructures.inventory.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/login")
    public String login() { return "login"; }

    /** Stockroom staff land on the scan screen, Purchasing on alerts, Admin on the material list. */
    @GetMapping("/")
    public String home(Authentication auth) {
        if (has(auth, "ROLE_STOCKROOM")) return "redirect:/scan";
        if (has(auth, "ROLE_PURCHASING")) return "redirect:/alerts";
        return "redirect:/materials";
    }

    private static boolean has(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}
