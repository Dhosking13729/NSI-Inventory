package com.nauticalstructures.inventory.web;

import com.nauticalstructures.inventory.domain.RequestStatus;
import com.nauticalstructures.inventory.service.BusinessRuleException;
import com.nauticalstructures.inventory.service.MaterialService;
import com.nauticalstructures.inventory.service.NotFoundException;
import com.nauticalstructures.inventory.service.ReorderService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/** Internal reorder-requests view: View Reorder Requests, Update Request Status (Purchasing, Admin). */
@Controller
@RequestMapping("/reorders")
public class ReorderController {

    private final ReorderService reorders;
    private final MaterialService materials;

    public ReorderController(ReorderService reorders, MaterialService materials) {
        this.reorders = reorders;
        this.materials = materials;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("open", reorders.open());
        model.addAttribute("closed", reorders.recentlyClosed());
        model.addAttribute("materials", materials.list());
        return "reorders";
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Integer id, @RequestParam(defaultValue = "") String status,
                         Authentication auth, RedirectAttributes flash) {
        try {
            RequestStatus next;
            try {
                next = RequestStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new BusinessRuleException("Choose Ordered or Closed");
            }
            var r = reorders.updateStatus(id, next, CurrentStaff.id(auth));
            flash.addFlashAttribute("success", "Reorder request #" + id + " for " + r.getMaterial().getMaterialName()
                    + " is now " + next.label() + ".");
        } catch (BusinessRuleException | NotFoundException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reorders";
    }

    @PostMapping
    public String create(@RequestParam(required = false) Integer materialId, @RequestParam(defaultValue = "") String quantity,
                         RedirectAttributes flash) {
        try {
            BigDecimal qty;
            try {
                qty = new BigDecimal(quantity.trim());
            } catch (NumberFormatException e) {
                throw new BusinessRuleException("Quantity must be a number");
            }
            var r = reorders.createManual(materialId, qty);
            flash.addFlashAttribute("success", "Reorder request #" + r.getRequestId() + " created for " + r.getMaterial().getMaterialName() + ".");
        } catch (BusinessRuleException | NotFoundException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reorders";
    }
}
