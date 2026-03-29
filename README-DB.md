# JSK Database Modules — Reference (jsk.21.7.4)

## Module Structure

```
db-modules/relational-modules/
├── jpa-hibernate-core/           # RdbProperties, RdbUtil (HikariCP DataSource), RdbType
├── jpa-spring-modules/
│   ├── jpa-spring-core/          # Spring Data JPA config, QueryDSL, transactions, UserTypes, base models
│   ├── jpa-spring-postgres-core/ # PG driver + dialect properties
│   ├── jpa-kv-store/             # Key-value store backed by JPA
│   └── pom.xml
├── jpa-util/                     # SQL→Java code generator ("JSA")
└── migration-modules/
    ├── relational-mig/           # Flyway MigratorBase
    └── postgres-mig/             # Adds PG driver to relational-mig
```

**Dependency flow**: `jpa-hibernate-core ← jpa-spring-core ← {jpa-spring-postgres-core, jpa-kv-store, jpa-util}`

---

## SQL-to-Class Generator (jpa-util)

**Entry point**: `sk.db.util.generator.JsaMain` — takes SQL `CREATE TABLE` scripts → generates JPA entities, ID classes, repos, storage facades.

### Pipeline

| Stage | Class | Role |
|---|---|---|
| Extract | `JsaTableInfoExtractor` | Parses SQL (JSqlParser): tables, columns, PKs, FKs, PG enums, meta-annotations from comments |
| Process | `JsaProcessor` | Transforms to entity models: Java types, composite keys, relations, enums, JSON fields |
| Export | `JsaExporter` | FreeMarker templates → Java source files |

### CLI / Programmatic Usage

```bash
java sk.db.util.generator.JsaMain <sqlFile> <outputDir> <schemaName> [explicitPrefix]
```
```java
JsaMain.startFileBased("/path/to/schema.sql", "/output/", "myschema", O.empty());
new JsaMain().start(sqlCode, fileInfo -> { /* process */ }, "myschema", O.empty());
```

### SQL Meta-Annotations

Comments in SQL control field mapping. Matched to tables by `(tableName, columnName)`.

| Annotation | Purpose | Extra Params |
|---|---|---|
| `@jsonb <table> <col> <JavaClass>` | JSONB → Java class | 1: class name |
| `@enum <table> <col> <EnumClass>` | VARCHAR → Java enum | 1: enum class |
| `@pg_enum <table> <col> <EnumClass>` | PG native enum → Java enum | 1: enum class |
| `@relationHere <table> <col> <refTable>` | FK to table in same SQL file | 1: referenced table |
| `@relationOutside <table> <col> <IdClass> <JpaClass>` | FK to entity outside SQL file | 2: ID class, JPA class |

### Input Example

```sql
CREATE TYPE enum_type AS ENUM ('t1','t2','t3');
CREATE TABLE abc_t1 (
    id TEXT NOT NULL PRIMARY KEY, info JSONB NOT NULL, some_enum enum_type NOT NULL,
    created_at TIMESTAMP, updated_at TIMESTAMP, version BIGINT NOT NULL
);
-- @jsonb abc_t1 info com.example.SomeInfoClass
-- @pg_enum abc_t1 some_enum com.example.SomeEnum

CREATE TABLE abc_t2 (
    id UUID NOT NULL PRIMARY KEY, type TEXT NOT NULL, t1_id TEXT NOT NULL,
    t0_id UUID NOT NULL, t2_id BYTEA NOT NULL, zzz TIMESTAMP,
    created_at TIMESTAMP, updated_at TIMESTAMP, version BIGINT NOT NULL
);
-- @enum abc_t2 type com.example.SomeEnum
-- @relationHere abc_t2 t1_id abc_t1
-- @relationOutside abc_t2 t0_id com.example.T0Id com.example.T0Jpa
```

### Generated Output

Files created under a temporary random package prefix (e.g., `__aBcDeFgHiJ/`) — **rename to your actual package**.

| Per Table | Template | Description |
|---|---|---|
| `<Entity>Jpa.java` | `jsa_entity_impl.ftl` | `@Entity` class with columns, types, converters |
| `<Entity>.java` | `jsa_entity_iface.ftl` | Interface with getters |
| `<Entity>Id.java` | `jsa_entity_id.ftl` | Typed ID (`IdUuid`/`IdString`/`IdLong`/`IdBase`) |
| `<Entity>JpaRepo.java` | `jsa_entity_repo.ftl` | `ReadWriteRepo` extension |

| Per Schema | Template | Description |
|---|---|---|
| `StorageFacade.java` | `jsa_storage_facade.ftl` | Interface: `getXById`, `getAllXByIds`, `newX` |
| `StorageFacadeImpl.java` | `jsa_storage_facade_impl.ftl` | Impl using repos + Q-classes |

Composite PKs also generate `<Entity>Id.java` (interface) + `<Entity>IdJpaImpl.java` (`@Embeddable`).

### Column Type Mapping (`JsaDbColumnType`)

| SQL Type | Java Type | UserType |
|---|---|---|
| `UUID` | `UUID` | `UTUuidIdToUuid` |
| `TEXT`/`VARCHAR` | `String` | `UTTextIdToVarchar` |
| `VARCHAR(36)` | `UUID` | `UTUuidIdToVarchar36` |
| `INTEGER`/`INT`/`INT4`/`SMALLINT` | `Integer` | `UTIntIdToInt` |
| `BIGINT`/`INT8` | `Long` | `UTLongIdToBigInt` |
| `DOUBLE PRECISION` | `Double` | — |
| `BOOLEAN`/`BOOL` | `Boolean` | — |
| `BYTEA` | `byte[]` | — |
| `TIMESTAMP` | `ZonedDateTime` | `UTZdtToTimestamp` |
| `JSONB` | `Object` | `UTObjectToJsonb` |
| PG enum types | `Object` | `UtPgEnumToEnumUserType` |

### Special Column Detection

- `version`/`row_version` → `@Version` (optimistic locking)
- `created_at`/`updated_at` → auto-managed via `JpaWithContextAndCreatedUpdated`
- PK columns → typed ID classes
- FK columns → `@ManyToOne(fetch = LAZY)` + `@JoinColumn`

---

## JPA/Hibernate Core (`jpa-hibernate-core`)

- **`RdbProperties`** — Interface: `getDriver()`, `getUrl()`, `getUser()`, `getPass()`, `getMaxPoolSize()`, `getJpaPackages()`, `getShowInfo()`, `getDialect()`
- **`RdbUtil`** — Creates HikariCP `DataSource` from `RdbProperties`. Supports port override via `RdbWithChangedPort`. Sets `stringtype=unspecified` for PG. Timeout: 30s.
- **`RdbType`** — Marker enum for port-change infrastructure (tests with dynamic ports)

---

## Spring JPA Integration (`jpa-spring-core`)

### `RdbBaseDbConfig` — Central `@Configuration`

Wires: `DataSource` → `EntityManagerFactory` (Hibernate) → `JpaTransactionManager` → `RdbTransactionWrapperImpl`, `EntityManagerProviderImpl`, `RdbIterator`, `TransactionalNamedParameterJdbcTemplate`.

### Package Scanning

`RdbJpaPackages` interface: any Spring bean implementing it adds its packages to entity scan. `RdbPropertiesBackedByJpaPackages` auto-collects all `RdbJpaPackages` beans.

### Base Entity Classes

- **`JpaWithContext`** — Base: `CoreServices ctx` (transient, injected), `boolean flush/detach`, implements `FlushableDetachable`
- **`JpaWithContextAndCreatedUpdated`** — Extends above; `@PrePersist`/`@PreUpdate` auto-set `createdAt`/`updatedAt` via `ServiceLocator4SpringImpl` (workaround for Hibernate Tuplizer issue). Subclasses (e.g. `JpaImportantLog`) can override and use `ctx` directly.

### Repository Interfaces

- **`ReadRepo<T, ID>`** — `Repository` + `JskQuerydslPredicateExecutor`
- **`ReadWriteRepo<T, ID>`** — `CrudRepository` + `ReadRepo` with explicit `findById`/`findAllById` overrides

---

## Custom Hibernate UserTypes

Package: `sk.db.relational.types`. Access Spring services via `UTWithContext.getInjector()` (reads `CoreServices` from session factory properties).

| Class | Mapping | Notes |
|---|---|---|
| `UTUuidIdToUuid` | UUID col → `IdUuid` subclass | Reflection for UUID constructor |
| `UTTextIdToVarchar` | TEXT/VARCHAR → `IdString` subclass | Reflection for String constructor |
| `UTUuidIdToVarchar36` | VARCHAR(36) → `IdUuid` subclass | UUID stored as text |
| `UTIntIdToInt` | INTEGER → typed ID | — |
| `UTLongIdToBigInt` | BIGINT → typed ID | — |
| `UTEnumToString` | VARCHAR → Java Enum | Stores enum name |
| `UtPgEnumToEnumUserType` | PG enum → Java Enum | Uses `Types.OTHER` |
| `UTObjectToJsonb` | JSONB → Java object | Via `IJson` service |
| `UTObjectToJsonbWithNulls` | JSONB → Java object | Preserves null JSON values |
| `UTStringToJsonb` | JSONB → raw String | — |
| `UTZdtToTimestamp` | TIMESTAMP → `ZonedDateTime` | Via `ITime` service |
| `UTZdtToBigInt` | BIGINT → `ZonedDateTime` | Epoch millis |
| `UTLocalDateToText` | TEXT → `LocalDate` | — |
| `UTStringSetToString` | TEXT → `Set<String>` | — |

---

## QueryDSL Integration

**Problem**: Spring Data's default `findAll(Predicate, Pageable)` runs `count(*)` on every call — expensive for large tables.

**Solution**: Custom implementation returning `List<T>` instead of `Page<T>`:

| Class | Role |
|---|---|
| `JskQuerydslPredicateExecutor<T>` | Custom interface: `findOne`, `findAll` (with predicate/ordering/pageable), `count` — all return `List`/`Optional`, no count queries |
| `JskQuerydslJpaRepositoryImpl<T>` | Implementation via JPQL queries |
| `JskJpaRepositoryFactory` | Detects repos extending `JskQuerydslPredicateExecutor`, injects custom impl |
| `JskJpaRepositoryFactoryBean` | Registered via `@EnableJpaRepositories(repositoryFactoryBeanClass=...)` |

**Package**: `org.springframework.data.jpa.repository.support` (intentional — needs access to Spring Data package-private internals).

Q-classes generated at compile time by `apt-maven-plugin` (`com.mysema.maven`).

### Usage Pattern (from `RdbKVStoreImpl`)

```java
BooleanExpression predicate = jpaKVItemWithRaw.id.key1.eq(new KVItemId(baseKey).getKey1());
predicate = predicate.and(jpaKVItemWithRaw.id.key2.goe(fromLastCategory.get()));
List<JpaKVItemWithRaw> values = kvRaw.findAll(predicate,
    QPageRequest.of(0, maxCount, jpaKVItemWithRaw.id.key2.asc()));
```

---

## Transaction Management

### Transaction Wrappers

- **`RdbTransactionWrapperRequiresNew`** — `transactionalForceNew(Supplier)` / `transactionalRunForceNew(Runnable)` with `REQUIRES_NEW` propagation
- **`RdbTransactionWrapper`** — Extends above + `transactional(Supplier)` / `transactionalRun(Runnable)` with default propagation
- **`RdbTransactionWrapperImpl`** — Implementation. Interfaces used independently (e.g. `RdbILogImpl` injects only `RequiresNew`).

### Transaction Manager

- **`RdbTransactionManager`** — Marker interface (compatibility), extends `ITransactionManager`
- **`RdbTransactionManagerImpl`** — Core implementation:
  - **Optimistic lock retry**: `IRepeat.repeat()`, up to 50 retries (100ms sleep) on: `ObjectOptimisticLockingFailureException`, `OptimisticLockException`, `OptimisticEntityLockException`, `StaleObjectStateException`
  - **`saveSingleItem(Object)`** — Abstract; subclasses implement with `switch` on entity type
  - **Flush/detach** — Entities implementing `FlushableDetachable` can request immediate flush/detach

Generated `StorageFacadeImpl` extends `RdbTransactionManagerImpl`:
```java
@Override protected void saveSingleItem(Object toSave) {
    switch(toSave) {
        case AbcT1Jpa e -> t1Repo.save(e);
        case AbcT2Jpa e -> t2Repo.save(e);
        default -> throw new IllegalStateException("Unexpected: " + toSave.getClass());
    }
}
```

- **`RdbTransactionManagerGenericImpl`** (2025+) — Auto-discovery alternative: `RdbTransactionManagerHolder` introspects all `ReadWriteRepo` beans at startup, builds `Class → ReadWriteRepo` map via reflection. No code generation needed for `saveSingleItem`.

---

## Context Injection into JPA Entities

`CoreServices` injected into every `JpaWithContext` entity via Hibernate event listeners:

1. `RdbIntegratorProvider4Context` → registers `RdbIntegrator4Context` as Hibernate `Integrator`
2. Listens to PRE_LOAD, PRE_INSERT, PRE_UPDATE, PERSIST, SAVE, MERGE, UPDATE
3. Injects `CoreServices` from session factory properties (key: `__JSK_INJECTOR__`)

Used by: `UTObjectToJsonb` (→`IJson`), `UTZdtToTimestamp` (→`ITime`), `JpaImportantLog` (→`ctx.times().nowZ()`)

---

## Built-in Database Services

Configured by `RdbLogPropsConfig` (`@Configuration` enabling repos in `sk.db.relational.spring.services.dao.repo`, packages in `sk.db.relational.model`).

### `JpaAppProperties` — DB-Backed Config (`app_configuration` table)

Composite PK: `propertyCategory + propertyId`. Fields: `value`, `propertyDate` (`UTZdtToBigInt`), `description`, `@Version`. Service: `RdbIAppConfigImpl` extends `ConfigCoreImpl`.

### `JpaImportantLog` — DB-Backed Logging (`important_log` table)

Fields: `ImportantLogId id` (UUID), `category`, `type`, `info` (JSON via `UTStringToJsonb`), `@Version counter`, `createdAt`, `updatedAt`. Service: `RdbILogImpl` implements `ILog`, uses `REQUIRES_NEW` transactions. Log types: `ILogType.AGG` (deterministic ID from hash) / `ILogType.LOG` (random UUID).

---

## Key-Value Store (`jpa-kv-store`)

Table: `_general_purpose_kv` — composite PK (`key1 TEXT`, `key2 TEXT`), `value TEXT`, `raw_value BYTEA`, `lock_date BIGINT`, `created_at`, `updated_at`, `version BIGINT`.

### Key Classes

| Class | Role |
|---|---|
| `KVItemId` | `@Embeddable` composite key. From `KvKey`: 0 cats→DEFAULT/DEFAULT, 1→DEFAULT/cat[0], 2→cat[0]/cat[1], 3+→cat[0]/join("_",rest) |
| `JpaKVItem` | Entity: `@EmbeddedId KVItemId`, value, lockDate, timestamps, `@Version` |
| `JpaKVItemWithRaw` | Same + `rawValue byte[]` |
| `RdbKVStoreImpl` | Implements `IKvStoreJsonBased` + `IKvLimitedStore` |

### Features

- **Optimistic locking**: retries up to 10,001 iterations; `updateString` also catches `DataIntegrityViolationException`
- **Distributed locking**: `tryLockOrRenew()` via `lock_date` column + raw JDBC
- **TTL infrastructure**: `checkTtl()` exists but currently invoked with null TTL (`t -> null`) — latent capability
- **Range queries**: `getRawVersionedListBetweenCategories()` via QueryDSL
- **Bulk clear**: `clearAll(except)` deletes all except specified keys

Config: `RdbKvConfig` — enables repos in `sk.db.kv`, registers `RdbKVStoreImpl` + `RdbJpaPackages` beans.

---

## Database Migration (`migration-modules`)

Flyway 11.20.2. `postgres-mig` = `relational-mig` + PG driver.

**`MigratorBase.migrate(String[] args)`** — accepts: JSON file path, raw JSON, resource path, or key=value CLI args.

**`MigratorModel`**: `connectionString`, `userName`, `password`, `resourceFolder`, `filePrefix` (default "V"), `migrationTableName`, `schemaName`.

---

## Cluster Sorter / Pagination Utilities

| Class | Purpose |
|---|---|
| `JcsQueryDslSource` | QueryDSL data source for cluster sorter: bidirectional pagination, configurable filter/ordering, dynamic offset |
| `JcsSqlSource` | Raw SQL alternative (FreeMarker templates + `EntityManager.createNativeQuery()`) |
| `JcsSqlBatch` | Batches multiple `JcsSqlSource` into single SQL via CTEs (`WITH ... UNION`) |
| `JcsDynamicPagingHelper` | Calculates optimal page size/index for offset+limit alignment |
| `RdbIterator` | Page-by-page processing: `iterate(consumer, repo, query, threadCount, ordering)`. Also: `getManyItemsByIds()` (batches of 10K for PG IN-clause limits), `getFastItemCountInPostgresDb()` (uses `pg_stat_all_tables.n_live_tup`) |

---

## PostgreSQL Support (`jpa-spring-postgres-core`)

`RdbPostgresProperties` extends `RdbPropertiesBackedByJpaPackages`: sets driver=`org.postgresql.Driver`, dialect=`org.hibernate.dialect.PostgreSQLDialect`. Extend and provide URL/user/pass/pool/showInfo.

---

## Setup Checklist

1. Create SQL schema with `CREATE TABLE` + meta-annotations
2. Run `JsaMain` → generates entities, repos, storage facade
3. **Rename generated package** from `__aBcDeFgHiJ/` to actual package
4. Extend `RdbPostgresProperties` with connection settings
5. Create `@Configuration` with `@EnableJpaRepositories(repositoryFactoryBeanClass = JskJpaRepositoryFactoryBean.class)`
6. Register `RdbJpaPackages` bean for entity packages
7. Create Flyway migrations via `MigratorBase`
8. Transaction manager: **code-generated** (`StorageFacadeImpl` extends `RdbTransactionManagerImpl`) or **auto-discovery** (`RdbTransactionManagerGenericImpl` + `RdbTransactionManagerHolder`)

### Hibernate Settings (from `RdbBaseDbConfig`)

| Setting | Value | Reason |
|---|---|---|
| `hibernate.hbm2ddl.auto` | `validate` | Never auto-create schema |
| `hibernate.enable_lazy_load_no_trans` | `true` | Lazy loading outside transactions |
| `org.hibernate.flushMode` | `COMMIT` | Flush only on commit |
| `hibernate.connection.handling_mode` | `DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION` | Pool optimization |

---

## Key Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| Hibernate | 6.6.41.Final | ORM |
| HikariCP | 7.0.2 | Connection pooling |
| Spring Data JPA | 3.5.8 | Repository abstraction |
| QueryDSL JPA | 6.12 (openfeign fork) | Type-safe queries |
| JSqlParser | 4.9 | SQL parsing (generator) |
| Flyway | 11.20.2 | Migrations |
| Jakarta Persistence | 3.1.0 | JPA standard |
