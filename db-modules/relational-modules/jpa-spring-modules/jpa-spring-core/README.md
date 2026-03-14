# JPA Spring Core

Central Spring JPA configuration providing transaction management, entity context injection, custom UserTypes, QueryDSL integration, and paginated iteration.

## What It Solves

- `RdbBaseDbConfig` — full Spring `@Configuration` for JPA/Hibernate with HikariCP, entity scanning, and transaction manager
- `RdbTransactionManagerImpl` — optimistic lock retry logic (catches `StaleObjectStateException` and retries)
- `JpaWithContextAndCreatedUpdated` — base entity with injected application context and automatic created/updated timestamps
- Custom QueryDSL integration that avoids `count(*)` overhead for pagination
- 14 Hibernate `UserType` implementations for JSK types (`O<T>`, `OneOf`, `ZonedDateTime`, etc.)

## Key Details

- Enforces strict bean configuration — `setAllowBeanDefinitionOverriding(false)` applies here too
- Entity context injection (`JpaWithContext`) lets entities access application services directly
- All timestamps use injected `ITime` — fully testable
