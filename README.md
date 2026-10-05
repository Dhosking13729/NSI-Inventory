# Nautical Structures Inventory & Materials Management System Enhancement

COM 430 Software Engineering – individual DevOps project (David Hosking).
Replaces the stockroom spreadsheet and paper logs with barcode/QR check-in and check-out, so production
planners can see real-time stock. Java 21 · Spring Boot 3.3 · PostgreSQL · GitHub Actions · AWS.

## Versions
| Version | Module | Features | Stage |
|---|---|---|---|
| **1.0.0** | 5 | Log in with roles · check in / check out by barcode or QR scan · material catalog · spreadsheet import with import log · staff user management | **TEST** |
| 2.0.0 | 6 | Low-stock alert engine · reorder thresholds · internal reorder-requests view · acknowledge/resolve alerts | STAGE |
| – | 7 | Security hardening, staging verification, release | PROD |

## DEV → TEST → STAGE → PROD
| Stage | Where | Database |
|---|---|---|
| DEV | IntelliJ IDEA on my workstation, `dev` profile | H2 file database |
| TEST | GitHub Actions on every push and pull request (`.github/workflows/ci.yml`); passing `main` builds saved to S3 | PostgreSQL 16 test database (CI service container) |
| STAGE | Docker container on AWS EC2 (Module 6) | Amazon RDS staging database |
| PROD | Docker container on AWS EC2 (Module 7) | Amazon RDS production database |

## Work on it
| Task | Command (Windows: use `mvnw.cmd`) |
|---|---|
| Run all tests + coverage | `./mvnw verify` → report in `target/site/jacoco/index.html` |
| Run the app (DEV) | `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` → http://localhost:8080, log in as `admin` / `dev-admin-password` |
| Load sample stock | Log in as admin → **Import** → upload `data/sample-inventory.csv` |
| Run in Docker with PostgreSQL | `docker compose up --build` → log in as `admin` / `local-admin-password` |

## Branching
1. Create a GitHub issue for the change, e.g. `#4 Check out material by scan`.
2. Branch from `main` named after it: `feature/4-check-out-scan`.
3. Commit, push, open a pull request into `main` (the template has the checklist).
4. CI must be green before merging. Merging into `main` saves the build to S3 – that build is in TEST.

## Layout
```
.github/workflows/ci.yml        DEV -> TEST pipeline
src/main/java/.../domain        Material, InventoryTransaction, StaffUser, ImportLog (data dictionary)
src/main/java/.../service       check-in/out, import, staff, materials
src/main/java/.../web           pages and security rules
src/main/resources/db/migration Flyway SQL (V1 = Version 1 tables)
src/main/resources/templates    screens (responsive HTML)
src/test/java                   27 unit, integration and web tests
data/sample-inventory.csv       spreadsheet export in import format
infra/aws-setup-test.md         S3 bucket + GitHub secrets for TEST
```
