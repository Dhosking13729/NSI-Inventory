package com.nauticalstructures.inventory.service;

import com.nauticalstructures.inventory.domain.Material;
import com.nauticalstructures.inventory.domain.MaterialType;
import com.nauticalstructures.inventory.repository.MaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Material catalog maintenance (Admin). */
@Service
@Transactional
public class MaterialService {

    private final MaterialRepository materials;

    public MaterialService(MaterialRepository materials) { this.materials = materials; }

    @Transactional(readOnly = true)
    public List<Material> list() { return materials.findAllByOrderByMaterialNameAsc(); }

    @Transactional(readOnly = true)
    public Material get(Integer id) {
        return materials.findById(id).orElseThrow(() -> new NotFoundException("No material #" + id));
    }

    public Material save(Integer id, String name, MaterialType type, String unit, BigDecimal quantityOnHand,
                         BigDecimal reorderThreshold, String barcodeValue) {
        Material m = id == null ? new Material() : get(id);
        String barcode = normalizeBarcode(barcodeValue);
        materials.findByBarcodeValueIgnoreCase(barcode)
                .filter(other -> !other.getMaterialId().equals(m.getMaterialId()))
                .ifPresent(other -> { throw new BusinessRuleException("Barcode " + barcode + " is already used by " + other.getMaterialName()); });
        apply(m, name, type, unit, quantityOnHand, reorderThreshold, barcode);
        return materials.save(m);
    }

    static String normalizeBarcode(String value) {
        if (value == null || value.isBlank()) throw new BusinessRuleException("BarcodeValue is required");
        String v = value.trim().toUpperCase();
        if (v.length() > 50) throw new BusinessRuleException("BarcodeValue can be at most 50 characters");
        return v;
    }

    /** Validates every field first, then sets them, so a bad value never leaves a material half-changed. */
    static void apply(Material m, String name, MaterialType type, String unit, BigDecimal qty, BigDecimal threshold, String barcode) {
        if (name == null || name.isBlank()) throw new BusinessRuleException("MaterialName is required");
        if (name.trim().length() > 100) throw new BusinessRuleException("MaterialName can be at most 100 characters");
        if (type == null) throw new BusinessRuleException("MaterialType is required");
        if (unit == null || unit.isBlank()) throw new BusinessRuleException("UnitOfMeasure is required");
        if (unit.trim().length() > 20) throw new BusinessRuleException("UnitOfMeasure can be at most 20 characters");
        BigDecimal onHand = Quantities.nonNegative(qty, "QuantityOnHand");
        BigDecimal reorderAt = Quantities.nonNegative(threshold, "ReorderThreshold");

        m.setMaterialName(name.trim());
        m.setMaterialType(type);
        m.setUnitOfMeasure(unit.trim());
        m.setQuantityOnHand(onHand);
        m.setReorderThreshold(reorderAt);
        m.setBarcodeValue(barcode);
    }
}
