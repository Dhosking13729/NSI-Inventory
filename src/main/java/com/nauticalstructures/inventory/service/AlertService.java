package com.nauticalstructures.inventory.service;

import com.nauticalstructures.inventory.domain.*;
import com.nauticalstructures.inventory.repository.LowStockAlertRepository;
import com.nauticalstructures.inventory.repository.MaterialRepository;
import com.nauticalstructures.inventory.repository.ReorderRequestRepository;
import com.nauticalstructures.inventory.repository.StaffUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Low-stock alert engine (use case: Generate Low-Stock Alert + Reorder Request, which «extends» Update Stock Level).
 * When a material's QuantityOnHand drops below its ReorderThreshold, the engine:
 *   1. raises one LowStockAlert (status Open) and notifies a purchasing staff member, and
 *   2. creates one ReorderRequest (status Pending) linked to that alert.
 * A material never has more than one active (Open or Acknowledged) alert at a time.
 */
@Service
@Transactional
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    static final EnumSet<AlertStatus> ACTIVE = EnumSet.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED);

    private final LowStockAlertRepository alerts;
    private final ReorderRequestRepository reorders;
    private final StaffUserRepository staff;
    private final MaterialRepository materials;

    public AlertService(LowStockAlertRepository alerts, ReorderRequestRepository reorders,
                        StaffUserRepository staff, MaterialRepository materials) {
        this.alerts = alerts;
        this.reorders = reorders;
        this.staff = staff;
        this.materials = materials;
    }

    /** Checks one material and returns the new alert if one was raised. {@code actor} is who caused the change. */
    public Optional<LowStockAlert> evaluate(Material material, StaffUser actor) {
        if (!material.isBelowThreshold()) return Optional.empty();
        if (material.getMaterialId() != null
                && alerts.existsByMaterialMaterialIdAndStatusIn(material.getMaterialId(), ACTIVE)) {
            return Optional.empty();
        }
        StaffUser notify = recipient(actor);
        LowStockAlert alert = alerts.save(new LowStockAlert(material, notify));
        reorders.save(new ReorderRequest(material, alert, recommendedQuantity(material)));
        log.warn("LOW STOCK: {} ({}) on hand {} is below threshold {}; alert #{} sent to {}",
                material.getMaterialName(), material.getBarcodeValue(), material.getQuantityOnHand().toPlainString(),
                material.getReorderThreshold().toPlainString(), alert.getAlertId(), notify.getUsername());
        return Optional.of(alert);
    }

    /** Checks every material (after a spreadsheet import). Returns how many alerts were raised. */
    public int sweep(StaffUser actor) {
        return (int) materials.findAllByOrderByMaterialNameAsc().stream()
                .map(m -> evaluate(m, actor)).filter(Optional::isPresent).count();
    }

    /**
     * On every start-up (for example right after the Version 2 upgrade, or a deploy to STAGE), check all materials so
     * stock that was already below its threshold gets an alert. Skipped while the system has no staff users yet.
     */
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void sweepOnStartup() {
        staff.findFirstByRoleOrderByStaffIdAsc(StaffRole.ADMIN).ifPresent(admin -> {
            int raised = sweep(admin);
            if (raised > 0) log.info("Start-up check raised {} low-stock alert(s)", raised);
        });
    }

    /**
     * Recommended reorder: enough to bring stock back up to twice the threshold.
     * Example: threshold 4, on hand 1 -> request 7 (1 + 7 = 8 = 2 x 4).
     */
    static BigDecimal recommendedQuantity(Material m) {
        return m.getReorderThreshold().multiply(BigDecimal.valueOf(2)).subtract(m.getQuantityOnHand()).setScale(2);
    }

    /** Who gets notified: the first Purchasing user; if none exist yet, the first Admin; otherwise whoever made the change. */
    StaffUser recipient(StaffUser actor) {
        return staff.findFirstByRoleOrderByStaffIdAsc(StaffRole.PURCHASING)
                .or(() -> staff.findFirstByRoleOrderByStaffIdAsc(StaffRole.ADMIN))
                .orElse(actor);
    }

    /** Acknowledge / Resolve Alert use case. */
    public LowStockAlert updateStatus(Integer alertId, AlertStatus next) {
        LowStockAlert alert = alerts.findById(alertId).orElseThrow(() -> new NotFoundException("No alert #" + alertId));
        if (next == null || !alert.getStatus().next().contains(next)) {
            throw new BusinessRuleException("Alert #" + alertId + " is " + alert.getStatus().label()
                    + " and can't be changed to " + (next == null ? "that status" : next.label()));
        }
        alert.setStatus(next);
        return alert;
    }

    @Transactional(readOnly = true)
    public List<LowStockAlert> active() { return alerts.findByStatusInOrderByAlertTimestampDescAlertIdDesc(ACTIVE); }

    @Transactional(readOnly = true)
    public List<LowStockAlert> recentlyResolved() {
        return alerts.findTop20ByStatusOrderByAlertTimestampDescAlertIdDesc(AlertStatus.RESOLVED);
    }

    @Transactional(readOnly = true)
    public List<LowStockAlert> forMaterial(Integer materialId) {
        return alerts.findByMaterialMaterialIdOrderByAlertTimestampDescAlertIdDesc(materialId);
    }

    @Transactional(readOnly = true)
    public long openCount() { return alerts.countByStatus(AlertStatus.OPEN); }
}
