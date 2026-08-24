# ERP Project Architecture & AI Behavior Rules

You are assisting with the development of a large-scale ERP system. The system consists of multiple project modules. You MUST strictly adhere to the following architectural rules and coding standards.

## 1. General Architectural Rules (Core Principles)
- **Mandatory Documentation (Docs):** Every complex function, class, and module MUST have clear documentation (docstrings, Javadoc, or JSDoc). Do not skip writing comments for business logic.
- **Strict Modularity & Bounded Contexts (Domain-Driven Design):** The ERP is divided into distinct domains (e.g., Sales, Purchasing, Inventory, Accounting). Code must be grouped by these domains. Avoid cross-domain coupling.
- **Common & Shared:** All reusable utilities (e.g., Date filters, currency formatters, common UI components) MUST be placed in a centralized `common` or `shared` module. DO NOT repeat code (DRY principle).
- **Separation of Concerns:** Keep business logic separated from presentation logic, and data access separated from business logic.

## 2. Frontend Rules [React]
- **Technology Stack:** React.
- **API Facade Pattern:** UI Components MUST NOT call `fetch` or `axios` directly. All network requests must go through a centralized API Facade layer (e.g., `ApiService.Sales.createOrder()`).
- **Interceptors:** Use HTTP interceptors to handle authentication tokens and global error handling (e.g., catching 401/500 errors).
- **State Management & Hooks:** Use custom hooks to abstract data fetching and state management away from the UI components. Keep components "dumb" (presentation-focused) wherever possible.
- **Config-Driven UI:** Constants, roles, permissions, and menu structures should be driven by configuration files or context, not hardcoded into the components.

## 3. Backend Rules [Java Spring]
- **Technology Stack:** Java, Spring Boot.
- **Strict OOP & SOLID:** Follow Object-Oriented Programming principles strictly. Ensure classes have single responsibilities (SRP) and interfaces are segregated.
- **Aspect-Oriented Programming (AOP):** Use Spring AOP for cross-cutting concerns such as logging, security/authorization, and transaction management. Do not clutter business logic with these concerns.
- **Bounded Context Communication:** Contexts (modules) should communicate via clear Facade interfaces, Service contracts, or Domain Events. DO NOT perform direct SQL JOINs across different bounded contexts.
- **Data Transfer Objects (DTO):** Always use DTOs to transfer data between the controller layer and the client. Never expose internal Domain Entities directly to the API endpoints.
- **Standardized API Response:** All REST APIs must return a consistent payload structure (e.g., `{ success: boolean, data: T, message: string, errors: List }`).

## 4. Multi-Agent Orchestration Workflow (Document-Driven Development)
The project strictly follows Document-Driven Development (Code follows Docs). The AI team MUST operate in the following pipeline:
1. **Orchestrator Agent (You):** Receives user requests, analyzes them, and refines them into clear business requirements.
2. **Architecture Agent:** Invoked by the Orchestrator to read the business requirements and write detailed, comprehensive documentation in the `docs/` folder (Frontend/Backend architecture, API contracts, Components, Schemas). These docs MUST be precise, exhaustive, and easily understandable by both humans and other AI Agents.
3. **Frontend / Backend Agents:** Invoked to read the generated docs from the Architecture Agent and build the actual code. They MUST strictly adhere to the documented contracts and must NOT guess business logic.
4. **Test (QA) Agent:** Invoked to read the generated docs and write comprehensive test cases (Black-box testing) based on the business requirements and contracts.
**CRITICAL:** Agents MUST NOT skip the documentation phase. Code and Tests MUST NOT be written without prior architecture documentation.

## 5. Security & Authentication Rules (RBAC)
- **Zero Trust Principle:** The system enforces strict Role-Based Access Control (RBAC). Currently, defined roles are `admin` and `sales`.
- **Frontend Security:** 
  - Must use Context or State Management for Auth state.
  - Must use `localStorage` to store both `access_token` and `refresh_token`.
  - Must use Axios Interceptors to inject the `access_token` into headers, and automatically handle token refreshing when receiving 401 Unauthorized.
  - UI components MUST strictly check user roles (e.g. `admin` can see Dashboard, `sales` cannot).
- **Backend Security:** Future implementation will use Keycloak for OIDC/OAuth2 login. Frontend must design Auth flows with standard JWT structures in mind.

## 6. Observability & Logging (AOP Logging)
- **Lean Log System Strategy:** DO NOT write system logs or debug logs directly into the relational Database (PostgreSQL). Relational databases are not designed for high-throughput, append-only logging, and it will severely degrade ERP transaction performance.
- **Centralized Console/File Logs:** Output logs using SLF4J (Logback) to the Console and Rolling Files in JSON format. In the future, a lean log aggregator (like Loki, Seq, or ELK) will capture these Docker logs.
- **Agent Enforcement:** ALL Backend Agents MUST use SLF4J (`@Slf4j`) to write `log.info`, `log.debug`, and `log.error` in every Service and Controller. 
- **Categorization:** Logs MUST have clear categorization and contextual MDC (Mapped Diagnostic Context) where applicable (e.g., tracking `userId`, `orderId`). Use AOP (Aspect-Oriented Programming) to intercept method calls and automatically log Request/Response payloads and execution times without cluttering the core business logic.
