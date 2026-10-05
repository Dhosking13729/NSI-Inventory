package com.nauticalstructures.inventory.service;

import com.nauticalstructures.inventory.domain.InventoryTransaction;
import com.nauticalstructures.inventory.domain.Material;
import com.nauticalstructures.inventory.domain.StaffUser;
import com.nauticalstructures.inventory.domain.TransactionType;
import com.nauticalstructures.inventory.repository.InventoryTransactionRepository;
import com.nauticalstructures.inventory.repository.MaterialRepository;
import com.nauticalstructures.inventory.repository.StaffUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Check In Material / Check Out Material use cases. Both include Update Stock Level; a check-out that takes
 * stock below its threshold is extended by Generate Low-Stock Alert + Reorder Request (Version 2).
 * A USB/Bluetooth scanner works as a keyboard: it types the BarcodeValue into the scan field and presses Enter.
 */
@Service
@Transactional
public class InventoryService {

    private final MaterialRepository materials;
    private final InventoryTransactionRepository transactions;
    private final StaffUserRepository staff;
    private final AlertService alertEngine;

    public InventoryService(MaterialRepository materials, InventoryTransactionRepository transactions,
                            StaffUserRepository staff, AlertService alertEngine) {
        this.materials = materials;
        this.transactions = transactions;
        this.staff = staff;
        this.alertEngine = alertEngine;
    }

    public record ScanResult(InventoryTransaction transaction, Optional<com.nauticalstructures.inventory.domain.LowStockAlert> alert) { }

    public ScanResult record(TransactionType type, String scannedCode, BigDecimal quantity, Integer staffId) {
        if (type == null) throw new BusinessRuleException("Choose Check-In or Check-Out");
        if (scannedCode == null || scannedCode.isBlank()) throw new BusinessRuleException("Scan a barcode or QR code");
        String code = scannedCode.trim();
        if (code.length() > 50) throw new BusinessRuleException("Scanned code is longer than 50 characters");
        BigDecimal qty = Quantities.positive(quantity, "Quantity");

        Material material = materials.findByBarcodeValueIgnoreCase(code)
                .orElseThrow(() -> new NotFoundException("Scanned code " + code + " does not match any material"));
        StaffUser user = staff.findById(staffId)
                .orElseThrow(() -> new NotFoundException("No staff user #" + staffId));

        updateStockLevel(material, type, qty);
        InventoryTransaction tx = transactions.save(new InventoryTransaction(material, type, qty, code, user));
        Optional<com.nauticalstructures.inventory.domain.LowStockAlert> alert = type == TransactionType.CHECK_OUT
                ? alertEngine.evaluate(material, user) : Optional.empty();
        return new ScanResult(tx, alert);
    }

    /** Included use case: Update Stock Level. Stock can never go below zero. */
    void updateStockLevel(Material material, TransactionType type, BigDecimal qty) {
        BigDecimal next = type == TransactionType.CHECK_IN
                ? material.getQuantityOnHand().add(qty)
                : material.getQuantityOnHand().subtract(qty);
        if (next.signum() < 0) {
            throw new BusinessRuleException("Cannot check out " + qty.stripTrailingZeros().toPlainString() + " "
                    + material.getUnitOfMeasure() + " of " + material.getMaterialName() + "; only "
                    + material.getQuantityOnHand().stripTrailingZeros().toPlainString() + " on hand");
        }
        if (next.compareTo(Quantities.MAX) > 0) throw new BusinessRuleException("Quantity on hand would be too large");
        material.setQuantityOnHand(next);
    }

    @Transactional(readOnly = true)
    public List<InventoryTransaction> recent() {
        return transactions.findTop15ByOrderByTransactionTimestampDescTransactionIdDesc();
    }

    @Transactional(readOnly = true)
    public List<InventoryTransaction> history(Integer materialId) {
        return transactions.findByMaterialMaterialIdOrderByTransactionTimestampDescTransactionIdDesc(materialId);
    }
}
