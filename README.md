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

*(Supports SpEL for `key` evaluation)*

### 2. Startup Utilities (`dev.gmky.utils.startup`)

#### `AppReadyLogging`
Automatically logs application access URLs (Local, External, Swagger) when the application starts.

### 3. Common Utilities (`dev.gmky.utils.common`)

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

### 4. Mappers (`dev.gmky.utils.mapper`)

#### `EntityMapper<D, E>`

Base MapStruct interface for Entity-DTO conversion.

- `toDto(E entity)`
- `toEntity(D dto)`
- `partialUpdate(D dto, E entity)`

## Installation

### Maven

```xml
<dependency>
    <groupId>dev.gmky</groupId>
    <artifactId>gmky-spring-utils</artifactId>
    <version>0.0.3</version>
</dependency>
```

### Gradle

```gradle
implementation 'dev.gmky:gmky-spring-utils:0.0.3'
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
