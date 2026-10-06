# Release notes

## 2.0.0 – Version 2, TEST → STAGE (Module 6)
**Use cases delivered:** Generate Low-Stock Alert + Reorder Request («extends» Update Stock Level); Configure Reorder
Threshold; View Reorder Requests; Update Request Status; Acknowledge / Resolve Alert.

**Database (Flyway V2):** `low_stock_alert`, `reorder_request`, as in the Submission 3 data dictionary. Upgrades a
Version 1 database in place; existing data is kept.

**Alert engine rules**
- Fires when QuantityOnHand drops **below** ReorderThreshold: after a check-out, a threshold change, an Admin edit,
  a spreadsheet import, and once at start-up (catches stock that was already low before the upgrade).
- One active (Open or Acknowledged) alert per material; each alert creates one Pending reorder request for
  2 × threshold − on hand.
- Notifies the first Purchasing user (falls back to the first Admin); Purchasing and Admin see the open-alert count
  in the top bar.

**Pipeline:** new `Deploy to STAGE` workflow deploys a build that passed TEST from S3 to Docker on EC2 with Amazon
RDS, smoke-tests it on the server, and rolls back automatically if it fails.

**Other:** friendly "not available to your role" page instead of a bare 403 error; tables fit laptop screens.

## 1.0.0 – Version 1, DEV → TEST (Module 5)
**Use cases delivered:** Log In; Check In Material (scan); Check Out Material (scan); Update Stock Level (included);
Import Spreadsheet Data; Manage Staff Users.

**Database (Flyway V1):** `material`, `inventory_transaction`, `staff_user`, `import_log`, as in the Submission 3 data dictionary.

**Design correction:** `ImportLog.ImportStatus` is `VARCHAR(25)`, not `VARCHAR(20)`, because the allowed value
"Completed with Errors" is 21 characters. Found by the automated tests (NSI-1).

**Coming in Version 2:** `LowStockAlert` and `ReorderRequest` tables, Generate Low-Stock Alert + Reorder Request,
Configure Reorder Threshold, View Reorder Requests, Update Request Status, Acknowledge/Resolve Alert.
Version 1 already highlights materials at or below their threshold on the materials list.
