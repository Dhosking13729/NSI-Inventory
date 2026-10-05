package com.nauticalstructures.inventory.service;

import com.nauticalstructures.inventory.domain.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The values stored in the database must match the data dictionary exactly. */
class LabelsTest {

    @Test
    void storedValuesMatchTheDataDictionary() {
        assertThat(new TransactionType.Converter().convertToDatabaseColumn(TransactionType.CHECK_IN)).isEqualTo("Check-In");
        assertThat(new TransactionType.Converter().convertToDatabaseColumn(TransactionType.CHECK_OUT)).isEqualTo("Check-Out");
        assertThat(new StaffRole.Converter().convertToEntityAttribute("Stockroom")).isEqualTo(StaffRole.STOCKROOM);
        assertThat(new ImportStatus.Converter().convertToDatabaseColumn(ImportStatus.COMPLETED_WITH_ERRORS)).isEqualTo("Completed with Errors");
        assertThat(MaterialType.fromLabel(" aluminum ")).isEqualTo(MaterialType.ALUMINUM);
        assertThat(StaffRole.ADMIN.authority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void unknownLabelIsRejected() {
        assertThatThrownBy(() -> MaterialType.fromLabel("Wood")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MaterialType.fromLabel(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
