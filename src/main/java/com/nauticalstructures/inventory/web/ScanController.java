package com.nauticalstructures.inventory.web;

import com.nauticalstructures.inventory.domain.InventoryTransaction;
import com.nauticalstructures.inventory.domain.TransactionType;
import com.nauticalstructures.inventory.service.BusinessRuleException;
import com.nauticalstructures.inventory.service.InventoryService;
import com.nauticalstructures.inventory.service.NotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/** Scan screen for Check In Material and Check Out Material. */
@Controller
@RequestMapping("/scan")
public class ScanController {

    private final InventoryService inventory;

    public ScanController(InventoryService inventory) { this.inventory = inventory; }

    @GetMapping
    public String form(Model model) {
        if (!model.containsAttribute("type")) model.addAttribute("type", "CHECK_OUT");
        model.addAttribute("recent", inventory.recent());
        return "scan";
    }

    @PostMapping
    public String scan(@RequestParam(defaultValue = "") String type, @RequestParam(defaultValue = "") String scannedCode,
                       @RequestParam(defaultValue = "") String quantity, Authentication auth, RedirectAttributes flash) {
        flash.addFlashAttribute("type", type);
        try {
            TransactionType txType = TransactionType.valueOf(type);
            BigDecimal qty = parse(quantity);
            InventoryTransaction tx = inventory.record(txType, scannedCode, qty, CurrentStaff.id(auth));
            flash.addFlashAttribute("success", tx.getTransactionType().label() + " recorded: "
                    + qty.stripTrailingZeros().toPlainString() + " " + tx.getMaterial().getUnitOfMeasure() + " of "
                    + tx.getMaterial().getMaterialName() + ". Now on hand: "
                    + tx.getMaterial().getQuantityOnHand().stripTrailingZeros().toPlainString() + ".");
        } catch (BusinessRuleException | NotFoundException e) {
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("scannedCode", scannedCode);
            flash.addFlashAttribute("quantity", quantity);
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", "Choose Check-In or Check-Out");
        }
        return "redirect:/scan";
    }

    private static BigDecimal parse(String quantity) {
        try {
            return new BigDecimal(quantity.trim());
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("Quantity must be a number");
        }
    }
}
