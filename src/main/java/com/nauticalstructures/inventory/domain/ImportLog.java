package com.nauticalstructures.inventory.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Data dictionary table: ImportLog - one row per spreadsheet migration run. */
@Entity
@Table(name = "import_log")
public class ImportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "import_id")
    private Integer importId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_by_staff_id")
    private StaffUser importedBy;

    @Column(name = "import_date", nullable = false)
    private Instant importDate;

    @Column(name = "records_imported", nullable = false)
    private int recordsImported;

    @Column(name = "records_rejected", nullable = false)
    private int recordsRejected;

    @Column(name = "import_status", nullable = false, length = 25)
    private ImportStatus importStatus;

    protected ImportLog() { }

    public ImportLog(String fileName, StaffUser importedBy, int recordsImported, int recordsRejected, ImportStatus status) {
        this.fileName = fileName;
        this.importedBy = importedBy;
        this.importDate = Instant.now();
        this.recordsImported = recordsImported;
        this.recordsRejected = recordsRejected;
        this.importStatus = status;
    }

    public Integer getImportId() { return importId; }
    public String getFileName() { return fileName; }
    public StaffUser getImportedBy() { return importedBy; }
    public Instant getImportDate() { return importDate; }
    public int getRecordsImported() { return recordsImported; }
    public int getRecordsRejected() { return recordsRejected; }
    public ImportStatus getImportStatus() { return importStatus; }
}
