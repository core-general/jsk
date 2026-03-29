# JPA Util

SQL-to-JPA code generator — takes SQL DDL as input and generates complete JPA entity classes, repositories, and composite keys via a 3-stage pipeline.

## What It Solves

- Parses SQL DDL using JSqlParser, extracts table/column metadata and meta-annotations
- Transforms SQL models to JPA entity models, handles naming conventions, composite keys, and relations
- Generates Java source files via 9 FreeMarker templates (entities, storage facades, primary keys, embedded keys)
- Supports foreign key relationships, enum types, and composite primary keys

## Key Details

- The "crown jewel" of the DB modules — a complete code generation pipeline from SQL DDL to full JPA stack
- Generates idiomatic JPA entities with JSK conventions
