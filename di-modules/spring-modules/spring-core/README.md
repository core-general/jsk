# Spring Core

Core Spring integration for JSK — provides application bootstrap, all fundamental service bean definitions, and strict Spring context configuration.

## What It Solves

- `SpringApp` bootstraps `AnnotationConfigApplicationContext` with profile selection, welcome text, and logging init
- `SpringCoreConfig` declares all core JSK service beans: `ITime`, `IRand`, `IAsync`, `IBytes`, `IHttp`, `IIds`, `IJson`, `ILog`, `IRepeat`, `IFree`, `ISizedSemaphore`, `IExcept`
- `CoreServices` aggregates all core service interfaces into a single injectable holder (`ICoreServices`)
- `ServiceLocator4SpringImpl` adapts Spring's `ApplicationContext` to JSK's `IServiceLocator`

## Key Details

- Enforces strict bean configuration — `setAllowBeanDefinitionOverriding(false)`, accidental overriding causes startup failures
- `ISizedSemaphore` limits to `min(maxMemory/5, 200MB)` with 5 partitions for backpressure
- Enforces single active profile only
