# Web Vaadin Common

Vaadin Flow UI component library — custom layout components, typed form fields, and field binding infrastructure.

## What It Solves

- Custom layout components: `YHorizontal`, `YVertical`, `YHtml`, `YRemovableDialog`, `YSvg`
- Typed form fields: `YStringField`, `YIntField`, `YLongField`, `YFloatField`, `YDoubleField`, `YUuidField`, `YPasswordField`
- Binding infrastructure: `YBindedField` → `YBindedBufferedField` (save/cancel) → `YBindedBufferedDialogField` (in dialog)
- `BaseWebAppInit` bootstraps Vaadin + Spring MVC application with Atmosphere (Push) support

## Key Details

- 29 Java files, uses `Y` prefix convention for all custom components
- `BaseWebAppInit` handles dual servlet setup (Vaadin + Spring MVC `DispatcherServlet`)
- Depends on `vaadin-spring-boot-starter`
