package com.nauticalstructures.inventory.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Data dictionary table: InventoryTransaction - one row per check-in or check-out scan. Never updated or deleted. */
@Entity
@Table(name = "inventory_transaction")
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Integer transactionId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    @Column(name = "transaction_type", nullable = false, length = 10)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "scanned_code", nullable = false, length = 50)
    private String scannedCode;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private StaffUser staff;

    @Column(name = "transaction_timestamp", nullable = false)
    private Instant transactionTimestamp;

    protected InventoryTransaction() { }

    public InventoryTransaction(Material material, TransactionType type, BigDecimal quantity,
                                String scannedCode, StaffUser staff) {
        this.material = material;
        this.transactionType = type;
        this.quantity = quantity;
        this.scannedCode = scannedCode;
        this.staff = staff;
        this.transactionTimestamp = Instant.now();
    }

    public Integer getTransactionId() { return transactionId; }
    public Material getMaterial() { return material; }
    public TransactionType getTransactionType() { return transactionType; }
    public BigDecimal getQuantity() { return quantity; }
    public String getScannedCode() { return scannedCode; }
    public StaffUser getStaff() { return staff; }
    public Instant getTransactionTimestamp() { return transactionTimestamp; }
}
