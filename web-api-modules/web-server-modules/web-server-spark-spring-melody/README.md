# Web Server Spark Spring Melody

The main server assembly — embedded Jetty server, Spring configuration, and JavaMelody monitoring integration. This is what applications depend on to run a JSK web server.

## What It Solves

- `WebJettyServerStarter` configures and starts embedded Jetty with multipart config, SameSite STRICT cookies, and custom error handler
- `WebSparkCoreConfig` — full Spring `@Configuration` registering all standard beans (server, filters, params, auth, monitoring)
- Registers JSK servlet + JavaMelody filter into Jetty context
- Auto-registers with remote melody collector on startup

## Key Details

- Named "spark" historically — uses SparkJava's filter mechanism for servlet-level routing, despite running on Jetty
- The actual HTTP server is Jetty, not SparkJava
