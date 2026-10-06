package com.nauticalstructures.inventory.service;

import com.nauticalstructures.inventory.domain.*;
import com.nauticalstructures.inventory.repository.MaterialRepository;
import com.nauticalstructures.inventory.repository.ReorderRequestRepository;
import com.nauticalstructures.inventory.repository.StaffUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests for View Reorder Requests / Update Request Status. */
@ExtendWith(MockitoExtension.class)
class ReorderServiceTest {

    @Mock ReorderRequestRepository reorders;
    @Mock MaterialRepository materials;
    @Mock StaffUserRepository staff;
    @InjectMocks ReorderService service;

    StaffUser pat = new StaffUser("Pat Purchasing", StaffRole.PURCHASING, "pat", "h");

    private ReorderRequest request() {
        Material m = new Material();
        MaterialService.apply(m, "Hex Bolt", MaterialType.FASTENER, "each", BigDecimal.ONE, BigDecimal.TEN, "NSI-1");
        return new ReorderRequest(m, null, new BigDecimal("19"));
    }

    @Test
    void pendingToOrderedToClosedRecordsWhoReviewedIt() {
        ReorderRequest r = request();
        when(reorders.findById(5)).thenReturn(Optional.of(r));
        when(staff.findById(2)).thenReturn(Optional.of(pat));

        service.updateStatus(5, RequestStatus.ORDERED, 2);
        assertThat(r.getRequestStatus()).isEqualTo(RequestStatus.ORDERED);
        assertThat(r.getReviewedBy()).isSameAs(pat);
        service.updateStatus(5, RequestStatus.CLOSED, 2);
        assertThat(r.getRequestStatus()).isEqualTo(RequestStatus.CLOSED);
    }

    @Test
    void closedIsFinalAndStatusesCannotGoBackwards() {
        ReorderRequest r = request();
        when(reorders.findById(5)).thenReturn(Optional.of(r));
        when(staff.findById(2)).thenReturn(Optional.of(pat));

        service.updateStatus(5, RequestStatus.ORDERED, 2);
        assertThatThrownBy(() -> service.updateStatus(5, RequestStatus.PENDING, 2)).isInstanceOf(BusinessRuleException.class);
        service.updateStatus(5, RequestStatus.CLOSED, 2);
        assertThatThrownBy(() -> service.updateStatus(5, RequestStatus.ORDERED, 2))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("Closed");
    }

    @Test
    void pendingCanBeClosedWithoutOrdering() {
        ReorderRequest r = request();
        when(reorders.findById(5)).thenReturn(Optional.of(r));
        when(staff.findById(2)).thenReturn(Optional.of(pat));
        service.updateStatus(5, RequestStatus.CLOSED, 2);
        assertThat(r.getRequestStatus()).isEqualTo(RequestStatus.CLOSED);
    }

    @Test
    void manualRequestNeedsAMaterialAndAPositiveQuantity() {
        Material m = request().getMaterial();
        when(materials.findById(3)).thenReturn(Optional.of(m));
        when(reorders.save(any())).thenAnswer(i -> i.getArgument(0));

        ReorderRequest r = service.createManual(3, new BigDecimal("25"));
        assertThat(r.getAlert()).isNull();
        assertThat(r.getRequestStatus()).isEqualTo(RequestStatus.PENDING);

        assertThatThrownBy(() -> service.createManual(3, BigDecimal.ZERO)).hasMessageContaining("greater than 0");
        when(materials.findById(-1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createManual(null, BigDecimal.ONE)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void unknownRequestIsNotFound() {
        when(reorders.findById(9)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateStatus(9, RequestStatus.CLOSED, 2)).isInstanceOf(NotFoundException.class);
    }
}
