package com.nauticalstructures.inventory.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Data dictionary table: Material. */
@Entity
@Table(name = "material")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_id")
    private Integer materialId;

    @Column(name = "material_name", nullable = false, length = 100)
    private String materialName;

    @Column(name = "material_type", nullable = false, length = 50)
    private MaterialType materialType;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure;

    @Column(name = "quantity_on_hand", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @Column(name = "reorder_threshold", nullable = false, precision = 10, scale = 2)
    private BigDecimal reorderThreshold = BigDecimal.ZERO;

    @Column(name = "barcode_value", nullable = false, unique = true, length = 50)
    private String barcodeValue;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    @PrePersist @PreUpdate
    void touch() { lastUpdated = Instant.now(); }

    /** True when stock has dropped below the reorder threshold - the condition that fires a low-stock alert. */
    public boolean isBelowThreshold() { return quantityOnHand.compareTo(reorderThreshold) < 0; }

    public Integer getMaterialId() { return materialId; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public MaterialType getMaterialType() { return materialType; }
    public void setMaterialType(MaterialType materialType) { this.materialType = materialType; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }
    public BigDecimal getQuantityOnHand() { return quantityOnHand; }
    public void setQuantityOnHand(BigDecimal quantityOnHand) { this.quantityOnHand = quantityOnHand; }
    public BigDecimal getReorderThreshold() { return reorderThreshold; }
    public void setReorderThreshold(BigDecimal reorderThreshold) { this.reorderThreshold = reorderThreshold; }
    public String getBarcodeValue() { return barcodeValue; }
    public void setBarcodeValue(String barcodeValue) { this.barcodeValue = barcodeValue; }
    public Instant getLastUpdated() { return lastUpdated; }
}
