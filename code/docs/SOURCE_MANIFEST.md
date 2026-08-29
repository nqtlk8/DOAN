# Source Manifest — v5

## Repository basis

Documentation audited against:

```text
code/docker-compose.yml
code/nginx-hq.conf
code/nginx-branch-tp1.conf
code/nginx-branch-tp2.conf

code/erp-platform/services/erp-backend/pom.xml
code/erp-platform/services/erp-backend/src/main/java/
code/erp-platform/services/erp-backend/src/main/resources/
code/erp-platform/services/erp-backend/src/test/
```

## Primary source files

### Runtime

- `docker-compose.yml`
- `nginx-hq.conf`
- `nginx-branch-tp1.conf`
- `nginx-branch-tp2.conf`
- `application.yml`
- `application-hq.yml`
- `application-branch.yml`

### Database

- `V1__init_schema.sql`
- `V2__seed_test_data.sql`
- `migration-branch/V9__branch_db_security.sql`

### Security

- `SecurityConfig.java`
- `JwtAuthenticationFilter.java`
- `JwtTokenProvider.java`
- `BranchScopedAspect.java`
- `IdempotencyAspect.java`

### Business modules

- `identity`
- `branch`
- `catalog`
- `crm`
- `order`
- `inventory`
- `analytics`

## Baseline facts

- 26 database tables in V1.
- 9 top-level backend modules/packages relevant to runtime: identity, branch, catalog, crm, order, inventory, analytics, common, infrastructure/system.
- Docker Compose defines 3 PostgreSQL containers, Redis, 3 Spring Boot containers, 2 Branch Nginx containers, ERP frontend and public web.
- Backend port inside containers: 8080.
- HQ host backend port: 8080.
- Branch host backend ports: 8081 and 8082.
