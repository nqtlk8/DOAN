# Post-Ledger Remediation Baseline
**Date:** 2026-09-17

## 1. Git Status
- Working tree: **Clean**.
- No uncommitted changes in backend, frontend, migration, or docs.

## 2. Backend Baseline
- **Command:** `.\mvnw test`
- **Result:** PASS (129/129 tests passed)
- **Note:** Tests are currently running against H2 memory database without Flyway enabled. Integration with real PostgreSQL and Flyway migration V1->V17 is currently **blocked/untested** in the automated pipeline.

## 3. Frontend Baseline
- **Command:** `npm install`
- **Result:** PASS (Successfully installed dependencies in monorepo).

- **Command:** `npx tsc --noEmit`
- **Result:** PASS (No TypeScript compilation errors found. However, logic bugs are masked by `any` types).

- **Command:** `npm run lint`
- **Result:** FAIL (`Cannot find package 'eslint-config-next'`). Project is Vite/React but references NextJS config. This will be noted as Technical Debt.

- **Command:** `npm run test -- --run`
- **Result:** PASS (5/5 tests in 3 test files).

## 4. Known Failures & Blockers
- **Eslint**: Broken configuration (`eslint-config-next` missing).
- **Flyway**: Migrations V16/V17 may contain UTF-16 encoding preventing them from executing against a real PostgreSQL DB, but it's completely ignored by `./mvnw test`.

### CHECK GATE 0: PASS
Baseline established successfully. No business code modifications were made.
