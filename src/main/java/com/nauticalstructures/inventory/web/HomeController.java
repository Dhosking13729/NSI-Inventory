package com.nauticalstructures.inventory.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/login")
    public String login() { return "login"; }

    /** Stockroom staff land on the scan screen; everyone else on the material list. */
    @GetMapping("/")
    public String home(Authentication auth) {
        boolean scanner = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STOCKROOM"));
        return scanner ? "redirect:/scan" : "redirect:/materials";
    }
}
