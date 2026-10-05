package com.nauticalstructures.inventory.web;

import com.nauticalstructures.inventory.domain.AlertStatus;
import com.nauticalstructures.inventory.service.AlertService;
import com.nauticalstructures.inventory.service.BusinessRuleException;
import com.nauticalstructures.inventory.service.NotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Low-stock alerts screen: Acknowledge / Resolve Alert (Purchasing, Admin). */
@Controller
@RequestMapping("/alerts")
public class AlertController {

    private final AlertService alerts;

    public AlertController(AlertService alerts) { this.alerts = alerts; }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("active", alerts.active());
        model.addAttribute("resolved", alerts.recentlyResolved());
        return "alerts";
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Integer id, @RequestParam(defaultValue = "") String status, RedirectAttributes flash) {
        try {
            AlertStatus next = parse(status);
            var alert = alerts.updateStatus(id, next);
            flash.addFlashAttribute("success", "Alert #" + id + " for " + alert.getMaterial().getMaterialName()
                    + " is now " + next.label() + ".");
        } catch (BusinessRuleException | NotFoundException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/alerts";
    }

    private static AlertStatus parse(String v) {
        try {
            return AlertStatus.valueOf(v);
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Choose Acknowledged or Resolved");
        }
    }
}
