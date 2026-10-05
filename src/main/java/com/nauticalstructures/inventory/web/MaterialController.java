package com.nauticalstructures.inventory.web;

import com.nauticalstructures.inventory.domain.Material;
import com.nauticalstructures.inventory.domain.MaterialType;
import com.nauticalstructures.inventory.service.BusinessRuleException;
import com.nauticalstructures.inventory.service.InventoryService;
import com.nauticalstructures.inventory.service.MaterialService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Map;

/** Material catalog: everyone can view; Admin adds and edits materials. */
@Controller
@RequestMapping("/materials")
public class MaterialController {

    private final MaterialService materials;
    private final InventoryService inventory;
    private final com.nauticalstructures.inventory.service.AlertService alerts;

    public MaterialController(MaterialService materials, InventoryService inventory,
                              com.nauticalstructures.inventory.service.AlertService alerts) {
        this.materials = materials;
        this.inventory = inventory;
        this.alerts = alerts;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("materials", materials.list());
        return "materials";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("material", materials.get(id));
        model.addAttribute("history", inventory.history(id));
        model.addAttribute("alerts", alerts.forMaterial(id));
        return "material-detail";
    }

    /** Configure Reorder Threshold (Purchasing, Admin). */
    @PostMapping("/{id}/threshold")
    public String threshold(@PathVariable Integer id, @RequestParam(defaultValue = "") String reorderThreshold,
                            org.springframework.security.core.Authentication auth, RedirectAttributes flash) {
        try {
            boolean alerted = materials.updateThreshold(id, number(reorderThreshold, "ReorderThreshold"), CurrentStaff.id(auth));
            flash.addFlashAttribute("success", "Reorder threshold updated to " + reorderThreshold.trim() + ".");
            if (alerted) flash.addFlashAttribute("warning", "Stock is below the new threshold, so a low-stock alert and reorder request were created.");
        } catch (BusinessRuleException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/materials/" + id;
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", Map.of());
        model.addAttribute("types", MaterialType.values());
        return "material-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        Material m = materials.get(id);
        model.addAttribute("materialId", id);
        model.addAttribute("form", Map.of(
                "materialName", m.getMaterialName(), "materialType", m.getMaterialType().name(),
                "unitOfMeasure", m.getUnitOfMeasure(), "quantityOnHand", m.getQuantityOnHand().toPlainString(),
                "reorderThreshold", m.getReorderThreshold().toPlainString(), "barcodeValue", m.getBarcodeValue()));
        model.addAttribute("types", MaterialType.values());
        return "material-form";
    }

    @PostMapping({"", "/{id}"})
    public String save(@PathVariable(required = false) Integer id, @RequestParam Map<String, String> form,
                       org.springframework.security.core.Authentication auth, Model model, RedirectAttributes flash) {
        try {
            Material saved = materials.save(id, form.get("materialName"), parseType(form.get("materialType")),
                    form.get("unitOfMeasure"), number(form.get("quantityOnHand"), "QuantityOnHand"),
                    number(form.get("reorderThreshold"), "ReorderThreshold"), form.get("barcodeValue"), CurrentStaff.id(auth));
            flash.addFlashAttribute("success", "Saved " + saved.getMaterialName());
            return "redirect:/materials/" + saved.getMaterialId();
        } catch (BusinessRuleException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("materialId", id);
            model.addAttribute("form", form);
            model.addAttribute("types", MaterialType.values());
            return "material-form";
        }
    }

    private static MaterialType parseType(String v) {
        try {
            return v == null || v.isBlank() ? null : MaterialType.valueOf(v);
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Unknown MaterialType");
        }
    }

    private static BigDecimal number(String v, String field) {
        if (v == null || v.isBlank()) throw new BusinessRuleException(field + " is required");
        try {
            return new BigDecimal(v.trim());
        } catch (NumberFormatException e) {
            throw new BusinessRuleException(field + " must be a number");
        }
    }
}
