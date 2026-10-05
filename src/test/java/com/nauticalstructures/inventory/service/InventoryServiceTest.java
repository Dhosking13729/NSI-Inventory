package com.nauticalstructures.inventory.service;

import com.nauticalstructures.inventory.domain.*;
import com.nauticalstructures.inventory.repository.InventoryTransactionRepository;
import com.nauticalstructures.inventory.repository.MaterialRepository;
import com.nauticalstructures.inventory.repository.StaffUserRepository;
import org.junit.jupiter.api.BeforeEach;
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

/** Unit tests for Check In / Check Out and the included Update Stock Level rule. */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock MaterialRepository materials;
    @Mock InventoryTransactionRepository transactions;
    @Mock StaffUserRepository staff;
    @InjectMocks InventoryService service;

    Material bolt;
    StaffUser stockroom = new StaffUser("Sam Stockroom", StaffRole.STOCKROOM, "sam", "hash");

    @BeforeEach
    void setUp() {
        bolt = new Material();
        MaterialService.apply(bolt, "3/8\" Marine-Grade Stainless Bolt", MaterialType.FASTENER, "each",
                new BigDecimal("12"), new BigDecimal("5"), "NSI-FST-0038");
    }

    private void found() {
        when(materials.findByBarcodeValueIgnoreCase("NSI-FST-0038")).thenReturn(Optional.of(bolt));
        when(staff.findById(1)).thenReturn(Optional.of(stockroom));
        when(transactions.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void checkInAddsToStockAndRecordsWhoScannedWhat() {
        found();
        InventoryTransaction tx = service.record(TransactionType.CHECK_IN, "  NSI-FST-0038 ", new BigDecimal("2.5"), 1);

        assertThat(bolt.getQuantityOnHand()).isEqualByComparingTo("14.5");
        assertThat(tx.getTransactionType()).isEqualTo(TransactionType.CHECK_IN);
        assertThat(tx.getQuantity()).isEqualByComparingTo("2.5");
        assertThat(tx.getScannedCode()).isEqualTo("NSI-FST-0038");
        assertThat(tx.getStaff()).isSameAs(stockroom);
        assertThat(tx.getTransactionTimestamp()).isNotNull();
    }

    @Test
    void checkOutSubtractsFromStock() {
        found();
        service.record(TransactionType.CHECK_OUT, "NSI-FST-0038", new BigDecimal("12"), 1);
        assertThat(bolt.getQuantityOnHand()).isEqualByComparingTo("0");
    }

    @Test
    void checkOutCannotTakeStockBelowZero() {
        when(materials.findByBarcodeValueIgnoreCase("NSI-FST-0038")).thenReturn(Optional.of(bolt));
        when(staff.findById(1)).thenReturn(Optional.of(stockroom));

        assertThatThrownBy(() -> service.record(TransactionType.CHECK_OUT, "NSI-FST-0038", new BigDecimal("12.01"), 1))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("only 12 on hand");
        assertThat(bolt.getQuantityOnHand()).isEqualByComparingTo("12");
        verify(transactions, never()).save(any());
    }

    @Test
    void unknownBarcodeIsRejected() {
        when(materials.findByBarcodeValueIgnoreCase("NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.record(TransactionType.CHECK_IN, "NOPE", BigDecimal.ONE, 1))
                .isInstanceOf(NotFoundException.class).hasMessageContaining("does not match");
    }

    @Test
    void quantityMustBePositiveWithAtMostTwoDecimals() {
        assertThatThrownBy(() -> service.record(TransactionType.CHECK_IN, "X", BigDecimal.ZERO, 1))
                .hasMessageContaining("greater than 0");
        assertThatThrownBy(() -> service.record(TransactionType.CHECK_IN, "X", new BigDecimal("-1"), 1))
                .hasMessageContaining("greater than 0");
        assertThatThrownBy(() -> service.record(TransactionType.CHECK_IN, "X", new BigDecimal("1.005"), 1))
                .hasMessageContaining("2 decimal places");
        verifyNoInteractions(materials, transactions);
    }

    @Test
    void scanAndTypeAreRequired() {
        assertThatThrownBy(() -> service.record(TransactionType.CHECK_IN, "  ", BigDecimal.ONE, 1))
                .hasMessageContaining("Scan a barcode");
        assertThatThrownBy(() -> service.record(null, "X", BigDecimal.ONE, 1))
                .hasMessageContaining("Check-In or Check-Out");
        assertThatThrownBy(() -> service.record(TransactionType.CHECK_IN, "X".repeat(51), BigDecimal.ONE, 1))
                .hasMessageContaining("longer than 50");
    }
}
