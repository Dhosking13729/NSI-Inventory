# Release notes

## 1.0.0 – Version 1, DEV → TEST (Module 5)
**Use cases delivered:** Log In; Check In Material (scan); Check Out Material (scan); Update Stock Level (included);
Import Spreadsheet Data; Manage Staff Users.

**Database (Flyway V1):** `material`, `inventory_transaction`, `staff_user`, `import_log`, as in the Submission 3 data dictionary.

**Design correction:** `ImportLog.ImportStatus` is `VARCHAR(25)`, not `VARCHAR(20)`, because the allowed value
"Completed with Errors" is 21 characters. Found by the automated tests (NSI-1).

**Coming in Version 2:** `LowStockAlert` and `ReorderRequest` tables, Generate Low-Stock Alert + Reorder Request,
Configure Reorder Threshold, View Reorder Requests, Update Request Status, Acknowledge/Resolve Alert.
Version 1 already highlights materials at or below their threshold on the materials list.
