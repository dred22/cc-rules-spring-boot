# ecommerce-api

## Stack
- Java 25
- Spring Boot 4.1.0
- Maven
- Postgresql 13.6

## Java 25
Target Java 25 (LTS); use modern features only where they make code clearer, safer, or shorter, and don't rewrite untouched code. Prefer `record` and `sealed` types for internal value types (not DTOs or entities — those follow the Lombok rules below), `switch` expressions with pattern matching (record deconstruction, `when` guards, `_`, no `default` on sealed types), `var` for obvious locals, text blocks, `Stream.toList()`, Gatherers, and sequenced collections (`getFirst`/`getLast`/`reversed`).
Use virtual threads for blocking I/O and `ScopedValue` instead of `ThreadLocal`; use plain loops over streams when there are side effects or checked exceptions.
Never use preview/incubator features (e.g. `StructuredTaskScope`) without asking; mention them in your response instead.

## Package Structure
Base package: `com.mk.ecommerce`

Sub-packages:
- `controller`
- `service`
- `repository`
- `entity`
- `dto`
- `exception`
- `config`

## Architecture Rules
- Always use DTOs in controllers — never expose entities directly.
- All business logic lives in the service layer — keep controllers thin.
- Centralised exception handling via `GlobalExceptionHandler`.
- All endpoints prefixed with `/api/v1/`.

## Lombok
- `@RequiredArgsConstructor` for injection everywhere (with `final` fields).
- Entities: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`.
- DTOs: `@Data @Builder @NoArgsConstructor @AllArgsConstructor`.

## Exceptions
All handled in `GlobalExceptionHandler`, returning `ErrorResponse` JSON:

| Exception                    | HTTP Status |
|------------------------------|-------------|
| `ResourceNotFoundException`  | 404         |
| `BusinessRuleException`      | 422         |
| `DuplicateResourceException` | 409         |
| `AccessDeniedException`      | 403         |
| `MethodArgumentNotValidException` | 400    |

- Validation errors (400) list all field errors in a `fieldErrors` array of `{field, message}`.
- `ErrorResponse` fields: `timestamp` (ISO-8601 UTC), `status`, `error`, `message`, `path`, optional `fieldErrors`.
- `AccessDeniedException` is the project's own class in the `exception` package, not Spring Security's — mind the import.

## Database
- Postgres, database `ecommerce_db`.
- `ddl-auto=validate` — tables come from `schema.sql`, not Hibernate.
- Money columns: `NUMERIC(19,2)` in `schema.sql`, `BigDecimal` in Java.

## Commands
(In PowerShell use `.\mvnw`; `./mvnw` works in Git Bash.)
- Run: `./mvnw spring-boot:run`
- Test: `./mvnw test`
- Compile check: `./mvnw clean compile`
