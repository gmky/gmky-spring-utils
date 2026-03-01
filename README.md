# GMK Spring Utils

Common utilities for Spring Boot framework.

## Description

This library provides common utilities and helper classes for Spring Boot applications.

## Features

### 1. Aspects (`dev.gmky.utils.execution`)

#### `@ExecutionTime`
Measures and logs method execution time.

```java

@ExecutionTime(name = "Payment", key = "#order.id")
public void processPayment(Order order) {
    // ...
}
// Logs: Method [Payment] - [123] executed in 150 ms
```

*(Supports SpEL for `key` evaluation with null-safe property access)*

### 2. Logging Utilities (`dev.gmky.utils.logging`)

#### `@LogPrefix`

Adds a custom prefix to all log messages within the annotated method's scope. This is particularly useful for request
tracing and log aggregation.

```java

@LogPrefix("'REQ-' + #userId")
public void process(String userId) {
    log.info("Processing data");
    // Output: ... [REQ-123] : Processing data
}
```

- **SpEL Support**: Evaluates the prefix dynamically using Spring Expression Language (supports null-safe property
  access like `#input.id`).
- **MDC Integration**: Automatically manages `logPrefix` in SLF4J MDC.
- **Automatic Cleanup**: Ensures the MDC is cleared after method execution.

### 3. Startup Utilities (`dev.gmky.utils.startup`)

#### `AppReadyLogging`
Automatically logs application access URLs (Local, External, Swagger) when the application starts.

### 4. Common Utilities (`dev.gmky.utils.common`)

#### `DateUtil`

Helper for generic date formatting.

- `formatDate(Date date, String pattern)`: Formats a date using system default timezone.

#### `ResponseUtil`

Standardized API response builder.

- `data(T data)`: Success response with body.
- `data(Page<T> page)`: Success response with pagination headers (`Link`, `X-TOTAL-COUNT`).
- `ok()`, `created()`, `noContent()`: Standard status responses.

#### `RequestUtil`

Access current HTTP request details statically.

- `getHeader(String name)`: Retrieve header value from current context.

#### `AppContextUtil`

Static access to Spring Beans.

- `getBean(Class<T> clazz)`: Retrieve bean by class.
- `getProperty(String key)`: Retrieve environment property.

### 5. HTTP Logging (`dev.gmky.utils.logging.http`)

Structured, configurable request/response logging for inbound servlet traffic and outbound HTTP clients. Disabled by default — enable via `gmky.logging.http.enabled=true`.

#### Components

- **`InboundHttpLoggingFilter`**: `OncePerRequestFilter` that logs inbound requests and responses. When `include-body=true`, the request body is eagerly buffered and replayed via a `CachedBodyHttpServletRequest` wrapper so downstream controllers still receive it. Response body is captured via `ContentCachingResponseWrapper`.
- **`OutboundRestTemplateInterceptor`**: `ClientHttpRequestInterceptor` for `RestTemplate`. Auto-wraps `RestTemplate` instances with `BufferingClientHttpRequestFactory` so the response body is readable for logging.
- **`OutboundWebClientFilter`**: `ExchangeFilterFunction` for `WebClient`. Response body logging is intentionally omitted to avoid blocking the reactive pipeline; register the filter manually via `WebClient.Builder.filter(outboundWebClientFilter)`.

#### Configuration

```yaml
gmky:
  logging:
    http:
      enabled: true
      inbound:
        enabled: true
        include-headers: true
        include-body: false          # true buffers the full request body
        max-body-size: 4096          # bytes before truncation
        exclude-paths:
          - /actuator/**
        exclude-headers:             # redacted as [REDACTED]
          - Authorization
          - Cookie
        log-level: DEBUG             # TRACE | DEBUG | INFO | WARN | ERROR
      outbound:
        enabled: true
        include-headers: true
        include-body: false
        max-body-size: 4096
        exclude-headers:
          - Authorization
        log-level: DEBUG
```

Log output format:
```
>>> INBOUND  [POST /api/users]
  Headers:
    Content-Type: application/json
    Authorization: [REDACTED]
  Request Body: {"name":"Alice"}

<<< INBOUND  [POST /api/users] -> 201 (45ms)
```

### 6. Mappers (`dev.gmky.utils.mapper`)

#### `EntityMapper<D, E>`

Base MapStruct interface for Entity-DTO conversion.

- `toDto(E entity)`
- `toEntity(D dto)`
- `partialUpdate(D dto, E entity)`

### 7. Spring Batch Utilities (`dev.gmky.utils.batch`)

Extensible framework for building robust batch jobs with minimal boilerplate.

#### Core Components

- **`BatchJobFactory`**: Fluent API for creating simple and multi-step jobs.
- **`AbstractDynamicProcessor`**: Base processor with hooks (`preProcess`, `validate`, `transform`, `postProcess`).
- **`AbstractDynamicWriter`**: Base writer with lifecycle hooks.

#### Smart Readers (JPA Pagination)

- **`JpaPagingReader`**: Memory-efficient JPQL reader that clears EntityManager.
- **`RepositoryPagingReader`**: Spring Data Repository reader.
- **`JpaSpecificationReader`**: Dynamic JPA Specification reader.

#### Optimized Writers

- **`JpaBatchWriter`**: Batch inserts/updates with periodic flushing.
- **`RepositoryWriter`**: Wrapper for `CrudRepository.saveAll`.

#### Example Usage

```java
@Bean
public Job userMigrationJob(BatchJobFactory factory, UserRepository repo) {
    return factory.createSimpleJob(
        "userMigration",
        new RepositoryPagingReader<>(repo, 100, Sort.by("id")),
        new AbstractDynamicProcessor<User, UserDTO>() {
            @Override
            protected UserDTO transform(User user) {
                return toDto(user);
            }
        },
        new JpaBatchWriter<>(entityManagerFactory, 100),
        BatchJobConfig.simple(100)
    );
}
```

### 8. CSV Utilities (`dev.gmky.utils.csv`)

High-performance, annotation-driven CSV-to-DTO mapping powered by OpenCSV.

#### Core Features
- **`OpenCsvStreamingReader`**: Read large CSVs lazily via `Stream` or `readAll` with customizable mapping.
- **`CsvBatchReader`**: Native Spring Batch `ItemReader` integration.
- **`@CsvRecord` & `@CsvColumn`**: Map CSV columns by precise name or sequence index.
- **Type Conversion**: Built-in temporal, numeric, and enum converters with extensible `TypeConverterRegistry`.
- **Validation**: Seamless `jakarta.validation` integration (e.g. Hibernate Validator).

```java
@CsvRecord(hasHeader = true, errorStrategy = ErrorStrategy.SKIP_AND_LOG)
public class UserDto {
    @CsvColumn(value = "Name", required = true)
    private String name;
    
    @CsvColumn("Age")
    private Integer age;
}

// Memory-safe stream reading
OpenCsvStreamingReader.forType(UserDto.class)
    .stream(inputStream)
    .filter(u -> u.getAge() > 18)
    .forEach(System.out::println);
```

## Installation

### Maven

```xml
<dependency>
    <groupId>dev.gmky</groupId>
    <artifactId>gmky-spring-utils</artifactId>
  <version>1.0.4</version>
</dependency>
```

### Gradle

```gradle
implementation 'dev.gmky:gmky-spring-utils:1.0.4'
```

## Requirements

- Java 21 or higher
- Spring Boot 3.2.1 or higher

## Publishing to Maven Central

This project uses the **Maven Central Portal** (modern) workflow.

### Prerequisites

1.  **Central Portal Account**: Register at [central.sonatype.com](https://central.sonatype.com/)
2.  **User Token**: Generate a token from your Account page.
3.  **GPG Key**: You must have a GPG key pair published to a keyserver.

### GitHub Secrets Configuration

Configure these secrets in your repository variables:

| Secret Name | Value |
|------------|-------|
| `OSSRH_USERNAME` | Set this to the string: `token` |
| `OSSRH_TOKEN` | Your long User Token from Central Portal |
| `GPG_PRIVATE_KEY` | Your exported private key |
| `GPG_PASSPHRASE` | Your GPG passphrase |

### Publishing Process

The library is automatically published when you create a GitHub Release.

#### Manual Production Deployment (Recommended)

1. Go to **Actions → Deploy to Prod**
2. Enter version (e.g., `0.0.1`)
3. Type `deploy`
4. This will tag the commit and publish to Central.

**Note**: The Central Portal **does not support SNAPSHOTS**. Always use release versions (e.g., `0.0.1`, `1.0.0-RC1`).

See [Environment Configuration](.github/ENVIRONMENTS.md) for detailed environment setup.

## Continuous Integration

### Running Tests

```bash
mvn test
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.
