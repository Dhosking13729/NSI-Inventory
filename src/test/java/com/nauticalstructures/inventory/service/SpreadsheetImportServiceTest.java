package com.nauticalstructures.inventory.service;

import com.nauticalstructures.inventory.domain.ImportStatus;
import com.nauticalstructures.inventory.domain.StaffRole;
import com.nauticalstructures.inventory.domain.StaffUser;
import com.nauticalstructures.inventory.repository.ImportLogRepository;
import com.nauticalstructures.inventory.repository.MaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;

/** Integration test: real Spring context and database (H2 locally, PostgreSQL in the TEST pipeline). */
@SpringBootTest
@Transactional
class SpreadsheetImportServiceTest {

    @Autowired SpreadsheetImportService importer;
    @Autowired StaffService staffService;
    @Autowired MaterialRepository materials;
    @Autowired ImportLogRepository logs;

    StaffUser admin;
    static final String HEADER = "MaterialName,MaterialType,UnitOfMeasure,QuantityOnHand,ReorderThreshold,BarcodeValue\n";

    @BeforeEach
    void admin() {
        admin = staffService.create("Ivy Manager", StaffRole.ADMIN, "ivy.import", "correct-horse-battery");
    }

    private ImportResult run(String csv) throws Exception {
        return importer.importCsv("inventory.csv", new StringReader(csv), admin.getStaffId());
    }

    @Test
    void cleanFileCompletesAndIsLogged() throws Exception {
        ImportResult r = run(HEADER + "A36 Steel Plate,Steel,sheet,9,4,nsi-stl-0250\nMarine Epoxy Primer,Coating,gal,7.5,5,NSI-COT-0101\n");

        assertThat(r.created()).isEqualTo(2);
        assertThat(r.log().getImportStatus()).isEqualTo(ImportStatus.COMPLETED);
        assertThat(r.log().getRecordsImported()).isEqualTo(2);
        assertThat(r.log().getRecordsRejected()).isZero();
        assertThat(r.log().getImportedBy().getUsername()).isEqualTo("ivy.import");
        assertThat(materials.findByBarcodeValueIgnoreCase("NSI-STL-0250")).get()
                .satisfies(m -> assertThat(m.getQuantityOnHand()).isEqualByComparingTo("9"));
        assertThat(logs.findAll()).hasSize(1);
    }

    @Test
    void badRowsAreRejectedByLineAndTheRestImported() throws Exception {
        ImportResult r = run(HEADER
                + "Good Bolt,Fastener,each,10,5,NSI-1\n"
                + "Bad Type,Wood,each,10,5,NSI-2\n"
                + "Negative,Steel,ft,-3,5,NSI-3\n"
                + "Not a number,Steel,ft,ten,5,NSI-4\n"
                + "No barcode,Steel,ft,1,1,\n"
                + "Three decimals,Coating,gal,1.005,1,NSI-5\n"
                + "Duplicate in file,Fastener,each,1,1,NSI-1\n");

        assertThat(r.log().getImportStatus()).isEqualTo(ImportStatus.COMPLETED_WITH_ERRORS);
        assertThat(r.log().getRecordsImported()).isEqualTo(1);
        assertThat(r.log().getRecordsRejected()).isEqualTo(6);
        assertThat(r.errors()).anyMatch(e -> e.startsWith("Line 3:") && e.contains("Wood"));
        assertThat(r.errors()).anyMatch(e -> e.startsWith("Line 8:") && e.contains("appears twice"));
    }

    @Test
    void reimportUpdatesExistingMaterialsByBarcode() throws Exception {
        run(HEADER + "Hex Bolt,Fastener,each,500,200,NSI-FST-M10\n");
        ImportResult second = run(HEADER + "Hex Bolt M10,Fastener,each,450.25,200,nsi-fst-m10\n");

        assertThat(second.created()).isZero();
        assertThat(second.updated()).isEqualTo(1);
        assertThat(materials.findByBarcodeValueIgnoreCase("NSI-FST-M10").get().getQuantityOnHand()).isEqualByComparingTo("450.25");
        assertThat(materials.findAll()).hasSize(1);
    }

    @Test
    void missingColumnsFailTheWholeImport() throws Exception {
        ImportResult r = run("MaterialName,BarcodeValue\nBolt,NSI-1\n");
        assertThat(r.log().getImportStatus()).isEqualTo(ImportStatus.FAILED);
        assertThat(r.errors().get(0)).contains("MaterialType").contains("QuantityOnHand");
        assertThat(materials.findAll()).isEmpty();
    }

    @Test
    void fileWithNoGoodRowsFails() throws Exception {
        assertThat(run(HEADER + "Bad,Wood,each,1,1,NSI-9\n").log().getImportStatus()).isEqualTo(ImportStatus.FAILED);
        assertThat(run(HEADER).log().getImportStatus()).isEqualTo(ImportStatus.FAILED);
    }

    @Test
    void sampleSpreadsheetImportsCleanly() throws Exception {
        try (var reader = java.nio.file.Files.newBufferedReader(java.nio.file.Path.of("data/sample-inventory.csv"))) {
            ImportResult r = importer.importCsv("sample-inventory.csv", reader, admin.getStaffId());
            assertThat(r.log().getImportStatus()).isEqualTo(ImportStatus.COMPLETED);
            assertThat(r.created()).isEqualTo(10);
        }
        assertThat(materials.findByBarcodeValueIgnoreCase("NSI-FST-0038").get().getMaterialName())
                .isEqualTo("3/8\" Marine-Grade Stainless Bolt");
    }
}
