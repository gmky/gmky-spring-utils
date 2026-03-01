# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build the project
mvn clean install

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=OpenCsvStreamingReaderTest

# Run a specific test method
mvn test -Dtest=OpenCsvStreamingReaderTest#testReadAll

# Generate test coverage report (output: target/site/jacoco/)
mvn test jacoco:report

# Build without tests
mvn clean install -DskipTests

# Build for Maven Central release (requires GPG key)
mvn clean deploy -Prelease
```

## Architecture Overview

This is a **Spring Boot auto-configuration library** (not an application). It registers beans automatically via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

### Auto-Configuration Entry Points

| Class | Activation Condition |
|-------|---------------------|
| `GmkyAutoConfiguration` | Always (core aspects + startup logging) |
| `BatchAutoConfiguration` | `spring-batch` on classpath |
| `CsvAutoConfiguration` | `CsvReader` class on classpath (always when library is included) |
| `HttpLoggingAutoConfiguration` | `gmky.logging.http.enabled=true` property |
| `InboundHttpLoggingAutoConfiguration` | HTTP logging enabled + servlet filter |
| `RestTemplateLoggingAutoConfiguration` | HTTP logging enabled + `RestTemplate` on classpath |
| `WebClientLoggingAutoConfiguration` | HTTP logging enabled + `spring-webflux` on classpath |

All Spring Boot dependencies are declared `<optional>true</optional>` in `pom.xml` — this library adds zero mandatory transitive deps beyond `commons-lang3`.

### Module Structure

**`dev.gmky.utils.execution`** — `@ExecutionTime` annotation + AOP aspect that logs method duration. Supports SpEL in the `key` attribute.

**`dev.gmky.utils.logging.annotation`** — `@LogPrefix` annotation + AOP aspect that sets an MDC key (`logPrefix`) for the duration of the annotated method. Supports SpEL.

**`dev.gmky.utils.logging.http`** — HTTP request/response logging split across three `@AutoConfiguration` classes to avoid `NoClassDefFoundError` when optional deps are absent. Configured via `gmky.logging.http.*` properties. Supports inbound (servlet filter), outbound RestTemplate (interceptor), and outbound WebClient (exchange filter).

**`dev.gmky.utils.batch`** — Spring Batch utilities:
- `BatchJobFactory` — fluent factory for single-step and multi-step jobs
- `AbstractDynamicProcessor` — lifecycle hooks: `preProcess → validate → transform → postProcess`
- `AbstractDynamicWriter` — lifecycle hooks around writing
- Readers: `JpaPagingReader` (JPQL), `RepositoryPagingReader` (Spring Data), `JpaSpecificationReader` (JPA Spec)
- Writers: `JpaBatchWriter` (bulk flush), `RepositoryWriter` (saveAll wrapper)

**`dev.gmky.utils.csv`** — Annotation-driven CSV mapping:
- `@CsvRecord` on DTO class — configures parser (delimiter, encoding, hasHeader, etc.)
- `@CsvColumn` on fields — maps by header name or column index
- `OpenCsvStreamingReader` — main entry point; supports `stream()`, `readAll()`, `readWithResult()`
- `CsvBatchReader` — wraps `OpenCsvStreamingReader` as a Spring Batch `ItemReader`
- `TypeConverterRegistry` — pluggable type converters; built-ins: String, Number, Boolean, Temporal, Enum, BigDecimal
- Error strategies on `CsvReaderConfig`: `FAIL_FAST`, `SKIP_AND_LOG`, `SKIP_SILENT`

**`dev.gmky.utils.common`** — Static utility singletons: `AppContextUtil`, `DateUtil`, `RequestUtil`, `ResponseUtil`.

**`dev.gmky.utils.mapper`** — `EntityMapper<D, E>` MapStruct base interface.

### Key Design Patterns

- **All feature beans use `@ConditionalOnMissingBean`** — consumers can override any bean by declaring their own.
- **AOP aspects follow interface + impl split**: `ExecutionTimeAspect`/`ExecutionTimeAspectImpl`, `LogPrefixAspect`/`LogPrefixAspectImpl`. Register interface type as `@ConditionalOnMissingBean` target.
- **HTTP logging auto-configs are split** so that the master `HttpLoggingAutoConfiguration` class has no bean methods referencing optional types — this prevents `NoClassDefFoundError` under Spring Framework 7+'s strict reflection.
- **SpEL evaluation** uses `NullSafePropertyAccessor` (in `dev.gmky.utils.core`) to allow safe `?.` navigation in annotation expressions.

### Adding a New Module

1. Create classes under `dev.gmky.utils.<module>/`
2. Create an `@AutoConfiguration` class with appropriate `@ConditionalOn*` guards
3. Register it in `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
4. If the module uses optional deps, keep the auto-config class free of bean methods that reference those types — use sibling `@AutoConfiguration` classes instead (follow the HTTP logging pattern)
