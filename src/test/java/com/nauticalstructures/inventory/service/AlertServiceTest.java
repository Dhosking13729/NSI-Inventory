package com.nauticalstructures.inventory.service;

import com.nauticalstructures.inventory.domain.*;
import com.nauticalstructures.inventory.repository.LowStockAlertRepository;
import com.nauticalstructures.inventory.repository.MaterialRepository;
import com.nauticalstructures.inventory.repository.ReorderRequestRepository;
import com.nauticalstructures.inventory.repository.StaffUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests for the low-stock alert engine (Generate Low-Stock Alert + Reorder Request). */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock LowStockAlertRepository alerts;
    @Mock ReorderRequestRepository reorders;
    @Mock StaffUserRepository staff;
    @Mock MaterialRepository materials;
    @InjectMocks AlertService engine;

    StaffUser sam = new StaffUser("Sam Stockroom", StaffRole.STOCKROOM, "sam", "h");
    StaffUser pat = new StaffUser("Pat Purchasing", StaffRole.PURCHASING, "pat", "h");
    StaffUser ivy = new StaffUser("Ivy Manager", StaffRole.ADMIN, "ivy", "h");

    private static Material material(String onHand, String threshold) {
        Material m = new Material();
        MaterialService.apply(m, "Antifouling Bottom Paint", MaterialType.COATING, "gal",
                new BigDecimal(onHand), new BigDecimal(threshold), "NSI-COT-0202");
        return m;
    }

    @Test
    void stockBelowThresholdRaisesOneOpenAlertAndAPendingReorderRequest() {
        Material paint = material("1", "4");
        when(staff.findFirstByRoleOrderByStaffIdAsc(StaffRole.PURCHASING)).thenReturn(Optional.of(pat));
        when(alerts.save(any())).thenAnswer(i -> i.getArgument(0));

        Optional<LowStockAlert> alert = engine.evaluate(paint, sam);

        assertThat(alert).isPresent();
        assertThat(alert.get().getStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(alert.get().getTriggeredThreshold()).isEqualByComparingTo("4");
        assertThat(alert.get().getNotifiedStaff()).isSameAs(pat);
        ArgumentCaptor<ReorderRequest> req = ArgumentCaptor.forClass(ReorderRequest.class);
        verify(reorders).save(req.capture());
        assertThat(req.getValue().getRequestStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(req.getValue().getAlert()).isSameAs(alert.get());
        assertThat(req.getValue().getRequestedQuantity()).isEqualByComparingTo("7");   // 2 x 4 - 1
    }

    @Test
    void stockAtOrAboveThresholdRaisesNothing() {
        assertThat(engine.evaluate(material("4", "4"), sam)).isEmpty();
        assertThat(engine.evaluate(material("9", "4"), sam)).isEmpty();
        assertThat(engine.evaluate(material("0", "0"), sam)).isEmpty();
        verifyNoInteractions(alerts, reorders);
    }

    @Test
    void neverTwoActiveAlertsForTheSameMaterial() {
        Material paint = mock(Material.class);
        when(paint.isBelowThreshold()).thenReturn(true);
        when(paint.getMaterialId()).thenReturn(7);
        when(alerts.existsByMaterialMaterialIdAndStatusIn(7, AlertService.ACTIVE)).thenReturn(true);

        assertThat(engine.evaluate(paint, sam)).isEmpty();
        verify(alerts, never()).save(any());
        verifyNoInteractions(reorders);
    }

    @Test
    void notifiesPurchasingThenAdminThenTheActor() {
        when(staff.findFirstByRoleOrderByStaffIdAsc(StaffRole.PURCHASING)).thenReturn(Optional.empty());
        when(staff.findFirstByRoleOrderByStaffIdAsc(StaffRole.ADMIN)).thenReturn(Optional.of(ivy));
        assertThat(engine.recipient(sam)).isSameAs(ivy);

        when(staff.findFirstByRoleOrderByStaffIdAsc(StaffRole.ADMIN)).thenReturn(Optional.empty());
        assertThat(engine.recipient(sam)).isSameAs(sam);
    }

    @Test
    void recommendedQuantityRefillsToTwiceTheThreshold() {
        assertThat(AlertService.recommendedQuantity(material("1", "4"))).isEqualByComparingTo("7");
        assertThat(AlertService.recommendedQuantity(material("0", "2.5"))).isEqualByComparingTo("5");
        assertThat(AlertService.recommendedQuantity(material("62.75", "100"))).isEqualByComparingTo("137.25");
    }

    @Test
    void alertStatusFollowsTheLifecycle() {
        LowStockAlert a = new LowStockAlert(material("1", "4"), pat);
        when(alerts.findById(1)).thenReturn(Optional.of(a));

        engine.updateStatus(1, AlertStatus.ACKNOWLEDGED);
        assertThatThrownBy(() -> engine.updateStatus(1, AlertStatus.OPEN)).isInstanceOf(BusinessRuleException.class);
        engine.updateStatus(1, AlertStatus.RESOLVED);
        assertThat(a.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThatThrownBy(() -> engine.updateStatus(1, AlertStatus.ACKNOWLEDGED))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Resolved");
        assertThatThrownBy(() -> engine.updateStatus(1, null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void startUpCheckRunsOnlyOnceStaffExist() {
        when(staff.findFirstByRoleOrderByStaffIdAsc(StaffRole.ADMIN)).thenReturn(Optional.empty());
        engine.sweepOnStartup();
        verifyNoInteractions(materials);

        when(staff.findFirstByRoleOrderByStaffIdAsc(StaffRole.ADMIN)).thenReturn(Optional.of(ivy));
        when(staff.findFirstByRoleOrderByStaffIdAsc(StaffRole.PURCHASING)).thenReturn(Optional.of(pat));
        when(materials.findAllByOrderByMaterialNameAsc()).thenReturn(java.util.List.of(material("1", "4"), material("9", "4")));
        when(alerts.save(any())).thenAnswer(i -> i.getArgument(0));
        engine.sweepOnStartup();
        verify(alerts, times(1)).save(any());
    }

    @Test
    void unknownAlertIsNotFound() {
        when(alerts.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> engine.updateStatus(99, AlertStatus.RESOLVED)).isInstanceOf(NotFoundException.class);
    }
}
