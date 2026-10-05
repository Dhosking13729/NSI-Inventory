package com.nauticalstructures.inventory.service;

import com.nauticalstructures.inventory.domain.ImportLog;
import com.nauticalstructures.inventory.domain.ImportStatus;
import com.nauticalstructures.inventory.domain.Material;
import com.nauticalstructures.inventory.domain.MaterialType;
import com.nauticalstructures.inventory.domain.StaffUser;
import com.nauticalstructures.inventory.repository.ImportLogRepository;
import com.nauticalstructures.inventory.repository.MaterialRepository;
import com.nauticalstructures.inventory.repository.StaffUserRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Import Spreadsheet Data use case (Admin). The legacy spreadsheet is saved as CSV with the columns
 * MaterialName, MaterialType, UnitOfMeasure, QuantityOnHand, ReorderThreshold, BarcodeValue.
 * Rows are matched on BarcodeValue (new rows are added, existing ones updated). A bad row is rejected with its
 * line number and never half-applied. Every run writes one ImportLog row, then the alert engine checks all materials.
 */
@Service
@Transactional
public class SpreadsheetImportService {

    static final List<String> COLUMNS = List.of("MaterialName", "MaterialType", "UnitOfMeasure",
            "QuantityOnHand", "ReorderThreshold", "BarcodeValue");

    private final MaterialRepository materials;
    private final ImportLogRepository importLogs;
    private final StaffUserRepository staff;
    private final AlertService alertEngine;

    public SpreadsheetImportService(MaterialRepository materials, ImportLogRepository importLogs, StaffUserRepository staff,
                                    AlertService alertEngine) {
        this.materials = materials;
        this.importLogs = importLogs;
        this.staff = staff;
        this.alertEngine = alertEngine;
    }

    public ImportResult importCsv(String fileName, Reader reader, Integer adminStaffId) throws IOException {
        StaffUser admin = staff.findById(adminStaffId).orElseThrow(() -> new NotFoundException("No staff user #" + adminStaffId));
        String name = (fileName == null || fileName.isBlank()) ? "upload.csv" : fileName;
        name = name.length() > 255 ? name.substring(0, 255) : name;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true)
                .setIgnoreEmptyLines(true).build();
        List<String> errors = new ArrayList<>();
        int created = 0, updated = 0;

        try (CSVParser parser = format.parse(reader)) {
            List<String> missing = COLUMNS.stream()
                    .filter(c -> parser.getHeaderNames().stream().noneMatch(h -> h.equalsIgnoreCase(c))).toList();
            if (!missing.isEmpty()) {
                errors.add("Missing column(s): " + String.join(", ", missing));
                return finish(name, admin, 0, 0, 0, errors, true);
            }
            List<String> seen = new ArrayList<>();
            for (CSVRecord row : parser) {
                long line = row.getRecordNumber() + 1;
                try {
                    String barcode = MaterialService.normalizeBarcode(row.get("BarcodeValue"));
                    if (seen.contains(barcode)) throw new BusinessRuleException("BarcodeValue " + barcode + " appears twice in the file");
                    Material m = materials.findByBarcodeValueIgnoreCase(barcode).orElse(null);
                    boolean isNew = m == null;
                    Material target = isNew ? new Material() : m;
                    MaterialService.apply(target, row.get("MaterialName"), MaterialType.fromLabel(row.get("MaterialType")),
                            row.get("UnitOfMeasure"), decimal(row.get("QuantityOnHand"), "QuantityOnHand"),
                            decimal(row.get("ReorderThreshold"), "ReorderThreshold"), barcode);
                    materials.save(target);
                    seen.add(barcode);
                    if (isNew) created++; else updated++;
                } catch (BusinessRuleException | IllegalArgumentException e) {
                    errors.add("Line " + line + ": " + e.getMessage());
                }
            }
        }
        return finish(name, admin, created, updated, errors.size(), errors, false);
    }

    private ImportResult finish(String fileName, StaffUser admin, int created, int updated, int rejected,
                                List<String> errors, boolean failedOutright) {
        int imported = created + updated;
        ImportStatus status = failedOutright || imported == 0 ? ImportStatus.FAILED
                : rejected > 0 ? ImportStatus.COMPLETED_WITH_ERRORS : ImportStatus.COMPLETED;
        ImportLog log = importLogs.save(new ImportLog(fileName, admin, imported, rejected, status));
        int alertsRaised = imported > 0 ? alertEngine.sweep(admin) : 0;   // imported stock may already be below threshold
        return new ImportResult(log, created, updated, errors, alertsRaised);
    }

    private static BigDecimal decimal(String raw, String field) {
        if (raw == null || raw.isBlank()) throw new BusinessRuleException(field + " is required");
        try {
            return new BigDecimal(raw.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            throw new BusinessRuleException(field + " is not a number (" + raw.trim() + ")");
        }
    }

    @Transactional(readOnly = true)
    public List<ImportLog> history() { return importLogs.findAllByOrderByImportDateDesc(); }
}
