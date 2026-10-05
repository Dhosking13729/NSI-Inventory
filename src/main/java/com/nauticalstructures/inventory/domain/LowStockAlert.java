package com.nauticalstructures.inventory.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Data dictionary table: LowStockAlert - raised by the alert engine when stock drops below its threshold. */
@Entity
@Table(name = "low_stock_alert")
public class LowStockAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Integer alertId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    @Column(name = "triggered_threshold", nullable = false, precision = 10, scale = 2)
    private BigDecimal triggeredThreshold;

    @Column(name = "alert_timestamp", nullable = false)
    private Instant alertTimestamp;

    @Column(nullable = false, length = 20)
    private AlertStatus status = AlertStatus.OPEN;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "notified_staff_id")
    private StaffUser notifiedStaff;

    protected LowStockAlert() { }

    public LowStockAlert(Material material, StaffUser notifiedStaff) {
        this.material = material;
        this.triggeredThreshold = material.getReorderThreshold();
        this.notifiedStaff = notifiedStaff;
        this.alertTimestamp = Instant.now();
    }

    public Integer getAlertId() { return alertId; }
    public Material getMaterial() { return material; }
    public BigDecimal getTriggeredThreshold() { return triggeredThreshold; }
    public Instant getAlertTimestamp() { return alertTimestamp; }
    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }
    public StaffUser getNotifiedStaff() { return notifiedStaff; }
}
