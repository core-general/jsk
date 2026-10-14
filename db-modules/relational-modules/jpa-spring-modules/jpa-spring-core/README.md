# JPA Spring Core

Central Spring JPA configuration providing transaction management, entity context injection, custom UserTypes, QueryDSL integration, and paginated iteration.

## What It Solves

- `RdbBaseDbConfig` — full Spring `@Configuration` for JPA/Hibernate with HikariCP, entity scanning, and transaction manager
- `RdbTransactionManagerImpl` — transaction retries for optimistic locking and confirmed uniqueness conflicts
- `JpaWithContextAndCreatedUpdated` — base entity with injected application context and automatic created/updated timestamps
- Custom QueryDSL integration that avoids `count(*)` overhead for pagination
- 14 Hibernate `UserType` implementations for JSK types (`O<T>`, `OneOf`, `ZonedDateTime`, etc.)

## Key Details

- Enforces strict bean configuration — `setAllowBeanDefinitionOverriding(false)` applies here too
- Entity context injection (`JpaWithContext`) lets entities access application services directly
- All timestamps use injected `ITime` — fully testable

## Creation and update retries

All `transactionWithSaveX*` overloads retry the complete transaction, including flush and commit, with at most 50 attempts and
100 ms between attempts. Confirmed conflicts include Spring `DuplicateKeyException`, Hibernate's `UNIQUE` constraint kind,
and PostgreSQL SQLSTATE `23505`, as well as the existing optimistic-lock failures. Classification traverses wrapped causes
and chained JDBC exceptions with cycle detection. Other constraints and business validation errors fail immediately.

Each owned attempt uses a fresh transaction and persistence context. Reload all authoritative state and revalidate inside
the lambda. After concurrent creation, the business lambda chooses reuse, update, or rejection; the framework never merges
conflicting creations automatically. Keep external side effects outside the retry boundary.

A call joining an existing transaction executes once and propagates failure to that transaction's owner. `ForceNew` variants
can retry independently, including inside an outer transaction. Exhaustion propagates the original first retryable exception;
non-retryable failures propagate unchanged. `IRepeat` remains unchanged.
