package com.nauticalstructures.inventory.web;

import com.nauticalstructures.inventory.domain.StaffRole;
import com.nauticalstructures.inventory.service.BusinessRuleException;
import com.nauticalstructures.inventory.service.StaffService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Manage Staff Users (Admin only). */
@Controller
@RequestMapping("/admin/users")
public class StaffController {

    private final StaffService staff;

    public StaffController(StaffService staff) { this.staff = staff; }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", staff.list());
        model.addAttribute("roles", StaffRole.values());
        return "users";
    }

    @PostMapping
    public String create(@RequestParam String name, @RequestParam String role, @RequestParam String username,
                         @RequestParam String password, Model model, RedirectAttributes flash) {
        try {
            staff.create(name, StaffRole.valueOf(role), username, password);
            flash.addFlashAttribute("success", "Added " + name.trim());
            return "redirect:/admin/users";
        } catch (BusinessRuleException | IllegalArgumentException e) {
            model.addAttribute("error", e instanceof BusinessRuleException ? e.getMessage() : "Choose a role");
            model.addAttribute("name", name);
            model.addAttribute("username", username);
            model.addAttribute("users", staff.list());
            model.addAttribute("roles", StaffRole.values());
            return "users";
        }
    }
}
