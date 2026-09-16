# CHANGELOG

## [Unreleased]

- **Sprint 2:** Standardized exception handling in Auth flow (`NoRoleAssignedException`, `SigningKeyNotConfiguredException`) and integrated with `GlobalExceptionHandler` (Status 401).
- **Sprint 3:** Bound JWT `branchId` to running instance via `JwtAuthenticationFilter`. Dropped old `@BranchScoped` aspect.
- **Sprint 4:** Fixed `ProductReader` blocking Admin users by returning `null` branch ID safely and skipping branch-specific pricing.
- **Sprint 5:** Updated `AnalyticsDataAdapter` to properly handle `branchId = null` as HQ company-wide scope using `NamedParameterJdbcTemplate`.
- **Sprint 6:** Fixed Replication issue by avoiding sync of snapshot tables (`stock_on_hand`, `receivable_debt`) and dynamically aggregating from `stock_movement` and `sales_invoice` for HQ scope.
