# JSK — Java Swiss Knife

Modular Java library providing utilities, services, DI integration, database tooling, web framework, AWS helpers, and more.

**Version:** `jsk.21.7.5`  
**Group ID:** `jsk`  
**Build:** `mvn -T 16 clean verify` (see [CLAUDE.md](CLAUDE.md) for details)  
**Maven S3 Repo:** see [AWSS3.md](AWSS3.md)

## Module Overview

| Module Group | Path | Description |
|---|---|---|
| **Core Utilities** | [`jx-modules/`](jx-modules/) | Foundation: collections (`Cc`), functional types (`O<T>`, `F1`, `X2`), services/DI, logging, testing, IP geo |
| **Text** | [`text-modules/`](text-modules/) | UTF-to-ASCII transliteration and text processing |
| **Math** | [`maths/`](maths/) | Common math utilities, JFreeChart integration |
| **Spring DI** | [`di-modules/`](di-modules/) | Spring integration: core config, properties plugin model |
| **Database** | [`db-modules/`](db-modules/) | JPA/Hibernate, Spring Data, Postgres, KV store, Flyway migrations, SQL→Java code generator |
| **Web Framework** | [`web-api-modules/`](web-api-modules/) | Interface-first web: annotations, server (Jetty/Spark), client proxies, Swagger generation, Vaadin |
| **AWS** | [`aws-modules/`](aws-modules/) | S3, DynamoDB, ECS, CDK, Translate, Spring integration |
| **External APIs** | [`outer-api-modules/`](outer-api-modules/) | Telegram, Facebook, Google Play, iOS Game Center |
| **Maven Plugins** | [`mvn-plugins/`](mvn-plugins/) | Spring properties plugin, web API info generator, Swagger client generator |
| **Cluster** | [`g-cluster/`](g-cluster/) | Distributed cluster: agent, checker, deployer |
| **Experiments** | [`z-module-4-experiments-only/`](z-module-4-experiments-only/) | Test server with all features wired together |

## Detailed Documentation

- **[README-UTILS.md](README-UTILS.md)** — `jx-modules` and `text-modules`: utilities, functional types, services, testing
- **[README-DB.md](README-DB.md)** — `db-modules`: JPA, Hibernate, Spring Data, migrations, SQL→Java generator
- **[README-WEB.md](README-WEB.md)** — `web-api-modules`: interface-first web framework, server, client, Swagger

Each module also has its own `README.md` inside its directory.

## Quick Start

```xml
<repositories>
    <repository>
        <id>jsk-repo</id>
        <url>https://jsk-maven-repository.s3.eu-north-1.amazonaws.com/release</url>
    </repository>
</repositories>
```

```xml
<dependency>
    <groupId>jsk</groupId>
    <artifactId>jx-utils</artifactId>
    <version>jsk.21.7.5</version>
</dependency>
```

## Building from Source

```bash
# If java_home.txt exists in the root:
export JAVA_HOME=$(cat java_home.txt)
mvn -T 16 clean verify

# Otherwise:
mvn -T 16 clean verify
```

## Test Server

The `z-module-4-experiments-only` module packages a runnable test server with all features wired:

```bash
cd z-module-4-experiments-only/target
java -Xmx500m -Dspring.profiles.active=default -jar z-module-test.jar
```

Server starts on port `8088`. See [CLAUDE.md](CLAUDE.md) for more details.
