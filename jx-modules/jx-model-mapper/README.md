# JX Model Mapper

Object-to-object mapping via ModelMapper with shallow/deep copy support, null handling, and built-in type converters.

## What It Solves

- `ModelMapperImpl` wraps `org.modelmapper` with JSK conventions for object mapping
- Supports shallow and deep copy modes
- Extensible via `ModelMapperConfig` for custom type converters

## Key Details

- Primarily used for DTO-to-entity and entity-to-DTO transformations
- Handles null properties gracefully
