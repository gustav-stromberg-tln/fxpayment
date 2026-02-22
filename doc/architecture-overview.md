# FX Payment Processor -- Architecture Overview

A full-stack web application for processing foreign exchange payments with multi-currency support, automatic fee calculation, IBAN validation, and idempotent payment creation.

## What It Does

Users submit payments through a browser-based form specifying an amount, currency (EUR/USD/GBP), recipient name, and IBAN. The backend calculates a processing fee, persists the payment, and displays it in a paginated history table. The system is idempotent: resubmitting the same payment (same `Idempotency-Key` header) returns the original result without creating a duplicate.

### Fee Rules

| Currency | Fee Rate | Minimum Fee |
|----------|----------|-------------|
| EUR      | 0%       | 0.00        |
| USD      | 1%       | 5.00        |
| GBP      | 1%       | 5.00        |

Formula: `fee = max(amount * feeRate, minimumFee)`. All calculations use `BigDecimal` with scale 4 and `HALF_UP` rounding. No floating-point arithmetic touches money at any layer.

---

## Tech Stack

| Layer    | Choice                               | Rationale                                                                                                                                       |
|----------|--------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| Backend  | Java 25, Spring Boot 3.5             | Mature ecosystem for REST APIs; virtual threads for I/O-bound throughput without thread-pool tuning                                             |
| Frontend | Angular 21, TypeScript 5.9 (strict)  | Standalone components, signal-based state, reactive forms with strong typing                                                                    |
| Database | PostgreSQL 18                        | `NUMERIC` types for exact decimal storage; `uuidv7()` for time-ordered primary keys; `CHECK` constraints and partial indexes for data integrity |
| ORM      | Hibernate / Spring Data JPA          | Parameterised queries (no SQL injection surface)                                                                                                |
| Migrations | Flyway                             | Version-controlled schema; Hibernate set to `ddl-auto: validate` so it never modifies the schema                                                |
| Caching  | Caffeine (in-process)                | Low-latency currency lookups and idempotency deduplication without an external cache service                                                    |
| UI       | Bootstrap 5.3                        | Consistent styling with minimal custom CSS for a form+table layout                                                                              |
| Infra    | Docker Compose, Nginx                | Single `docker compose up` brings up database, backend, and frontend; Nginx serves static files and proxies `/api/`                             |

---

## Architecture

```
Browser :4200 --> Nginx/Dev Proxy --> Spring Boot :8080 --> PostgreSQL :5432
```

The frontend is a single-page Angular app. It communicates with the backend exclusively through three REST endpoints:

| Method | Endpoint             | Purpose                       |
|--------|----------------------|-------------------------------|
| POST   | `/api/v1/payments`   | Create a payment (idempotent) |
| GET    | `/api/v1/payments`   | List payments (paginated)     |
| GET    | `/api/v1/currencies` | List supported currencies     |

### Backend Layers

```
Controller --> Service --> Repository --> PostgreSQL
```

- **Controllers** handle HTTP concerns only: deserialisation, validation annotations, status codes. No business logic.
- **Services** are split by responsibility: `FeeCalculationService` (fee math), `PaymentValidationService` (decimal precision checks), `PaymentService` (orchestration), `CurrencyService` (business-facing currency operations and decimal lookups), `CurrencyLookupService` (cached DB lookups), `IdempotencyCacheService` (deduplication).
- **Repositories** are Spring Data JPA interfaces. The only non-inherited method is the derived query `findByIdempotencyKey` on `PaymentRepository`.

### Frontend Structure

Two components rendered side-by-side in a single view (no router):

- **PaymentFormComponent** -- Reactive form with dynamic validation. Currency selection adjusts the amount field's step and min validators based on the currency's decimal precision. Owns the idempotency key lifecycle: generates a UUID on construction, reuses it across retries, regenerates after success.
- **PaymentHistoryComponent** -- Paginated table driven by Angular signals. Reloads automatically when a payment is created via a `Subject<void>` event stream from `PaymentService`.

State management uses Angular Signals with no external library. In `PaymentHistoryComponent`, the `currentPage` signal is converted to an observable via `toObservable()`, then `merge`d with the `paymentCreated$` Subject and fed through `switchMap` to trigger HTTP fetches.

---

## Database Schema

Two tables managed by Flyway migrations:

### `currencies` (reference data)

| Column      | Type           | Notes |
|-------------|----------------|-------|
| `code`      | `VARCHAR(3)` PK | ISO 4217 currency code |
| `name`      | `VARCHAR(255)` | Display name |
| `fee_rate`  | `NUMERIC(9,6)` | Percentage as decimal (0.01 = 1%) |
| `minimum_fee` | `NUMERIC(19,4)` | Floor for fee calculation |
| `decimals`  | `SMALLINT`     | ISO 4217 minor units (0-4); enforced by CHECK constraint |
| `created_at` | `TIMESTAMPTZ` | Set by Hibernate `@CurrentTimestamp` on insert |
| `updated_at` | `TIMESTAMPTZ` | Set by Hibernate `@CurrentTimestamp` on insert and update |

### `payments` (transaction records)

| Column            | Type            | Notes |
|-------------------|-----------------|-------|
| `id`              | `UUID` PK       | `DEFAULT uuidv7()` -- time-ordered for index locality |
| `idempotency_key` | `VARCHAR(36)` NOT NULL | Unique index |
| `amount`          | `NUMERIC(19,4)` | Transaction amount at internal precision |
| `currency`        | `VARCHAR(3)` FK | References `currencies(code)` |
| `recipient`       | `VARCHAR(140)`  | CHECK: length >= 2 |
| `recipient_account` | `VARCHAR(255)` | IBAN |
| `processing_fee`  | `NUMERIC(19,4)` | Calculated fee stored alongside the payment |
| `status`          | `VARCHAR(20)`   | CHECK constraint limits to PENDING/PROCESSING/COMPLETED/FAILED/REFUNDED |
| `created_at`      | `TIMESTAMPTZ`   | Set by Hibernate `@CurrentTimestamp` on insert; descending index for paginated listing |
| `updated_at`      | `TIMESTAMPTZ`   | Set by Hibernate `@CurrentTimestamp` on insert and update |

### Schema Design Decisions

**`NUMERIC(19,4)` for money.** 19 digits of precision with 4 decimal places handles all ISO 4217 currencies (including 3-decimal currencies like BHD) and avoids floating-point rounding errors. Internal calculations use scale 4; API responses round to the currency's display decimals.

**UUIDv7 primary keys.** Time-ordered UUIDs keep the B-tree index append-mostly, avoiding page splits that random UUIDs cause. PostgreSQL 18 supports `uuidv7()` natively.

**Descending time index.** `idx_payments_by_time` indexes `created_at DESC` for efficient paginated listing sorted by newest first.

**Foreign key on `currency`.** Prevents payments referencing non-existent currencies. The currency table is the source of truth for fee configuration.

**Application-managed timestamps.** Both `created_at` and `updated_at` are managed by Hibernate via `@CurrentTimestamp(event = EventType.INSERT)` and `@CurrentTimestamp(event = {EventType.INSERT, EventType.UPDATE})` respectively. Hibernate generates the timestamp value before building the INSERT/UPDATE SQL, so the column is always populated. The database columns still carry a `DEFAULT CURRENT_TIMESTAMP`, but this only applies to raw SQL statements that omit the column entirely. In a single-owner microservice this is simpler than database triggers: timestamp behaviour is visible in the entity class, testable without a database, and avoids hidden side effects that triggers introduce.

**Fee configuration in the database, not code.** Fee rates and minimums are columns on the `currencies` table rather than application constants. Adding a new currency or changing a fee rate is a data change, not a code deployment.

---

## Observability

### Request Correlation

Every HTTP request is assigned a unique request ID via `RequestCorrelationFilter`, a servlet filter registered at the highest precedence. The filter:

1. Reads the `X-Request-Id` header from the incoming request. If absent or blank, generates a new UUID.
2. Stores the request ID in SLF4J's MDC under the key `requestId`, making it available to every log statement for the duration of the request.
3. Sets the `X-Request-Id` response header so clients can correlate their request with server-side logs.
4. Clears the MDC entry in a `finally` block to prevent leaking across thread-reused virtual threads.

The logging pattern includes `[%X{requestId:-}]` so every log line emitted during request processing carries the correlation ID. This makes it possible to `grep` all log lines for a single payment flow end-to-end.

### Spring Boot Actuator

Spring Boot Actuator exposes operational endpoints under `/actuator/`:

| Endpoint            | Purpose                                    |
|---------------------|--------------------------------------------|
| `/actuator/health`  | Liveness/readiness check (returns `UP`)    |
| `/actuator/info`    | Application metadata                       |
| `/actuator/metrics` | JVM, HTTP, and Spring-managed metrics      |

Only `health`, `info`, and `metrics` are exposed. Sensitive endpoints like `env`, `beans`, and `configprops` are not exposed. Health endpoint details are gated behind authorisation (`show-details: when-authorized`).

### Startup Configuration Logging

`StartupConfigLogger` listens for `ApplicationReadyEvent` and logs the effective runtime configuration at `DEBUG` level:

- Datasource URL (with credentials masked if embedded in the JDBC URL)
- Server port
- Flyway enabled status
- CORS allowed origins and methods
- Cache TTLs and max sizes (currency and idempotency)

Logging at `DEBUG` keeps production logs clean by default while remaining available when needed (set `logging.level.com.fxpayment.config.StartupConfigLogger=DEBUG`). The datasource URL is sanitised by stripping any `user:password@` component before logging.

---

### Observability Out of Scope Functionality

The following observability capabilities are intentionally excluded from this test project:

- **Prometheus/Micrometer metrics export.** The Actuator `/metrics` endpoint provides JVM and HTTP metrics in its own format, but no Prometheus scrape endpoint (`/actuator/prometheus`) or Micrometer registry is configured.
- **OpenTelemetry tracing.** No distributed tracing (OpenTelemetry, Zipkin, Jaeger) is configured. The MDC-based request ID provides basic request correlation within a single service, but cross-service trace propagation, span collection, and trace visualisation are out of scope.

---

## Key Architectural Trade-offs

### Idempotency via cache + unique index

The idempotency key is checked in a Caffeine cache (24h TTL, 10k entries) before hitting the database. The cache is populated lazily: a miss queries the database via `findExistingPayment()`, and the result is cached for subsequent lookups (`@Cacheable`). Empty results are never cached, so a first-time payment always falls through to the database. The database unique index is the ultimate safeguard: if two concurrent requests race past the cache lookup, the loser gets a `DataIntegrityViolationException`, catches it, retries, and finds the winner's record.

**Trade-off:** The cache is in-process, so it doesn't work across multiple backend instances. For a single-node deployment this is simpler and faster than Redis. Scaling horizontally would require switching to a distributed cache or relying solely on the database constraint.

### Currency cache: TTL expiry, not manual invalidation

Currency data is cached with a 24h TTL and no explicit eviction.

**Trade-off:** When a currency admin endpoint is introduced, manual cache eviction will become necessary to avoid serving stale fee rates for up to 24 hours (annotating the write method with `@CacheEvict(value = {"currencyByCode", "allCurrencies"}, allEntries = true)`). Until that mutation path exists, eviction infrastructure would be dead code with no trigger.

### H2 for tests vs. PostgreSQL for production

Tests run against H2 in-memory with Flyway disabled and `ddl-auto: create-drop`. This is fast but means tests don't exercise PostgreSQL-specific features: `uuidv7()`, `plpgsql` triggers, `CHECK` constraints, or partial indexes. The trade-off favours developer speed over fidelity. A Testcontainers-based PostgreSQL setup would close this gap at the cost of slower test runs.

### No routing in the frontend

The application has no Angular Router. Both components render on a single page. This means no lazy loading, no deep linking, and no route guards. For a two-component app this is the right call -- adding a router would be pure ceremony. If additional views are needed, introducing routing is straightforward since all components are already standalone.

### No authentication or authorisation

Intentionally omitted. There is no user model, no session state, no JWT/OAuth. Spring Security is not included because there is nothing to secure against without an identity system. Adding it would mean immediately disabling most defaults. All payments are globally visible.

### Signals over NgRx

Angular Signals handle all frontend state. For two components with a handful of signals each, NgRx would add significant boilerplate (actions, reducers, effects, selectors) with no benefit. The `Subject<void>` pattern for cross-component events is minimal and tested.

### No soft-delete — payments are immutable

Payments are never deleted. Once a payment is created, it remains in the database permanently and is always retrievable by its idempotency key. This ensures that idempotency guarantees are never broken: a payment with a given idempotency key was already processed and cannot be created again. Any business level status changes (refunds, cancellations) are expressed via the `status` column, not by hiding rows. Production would need a retention or archival policy for old records.

### Payment status is always COMPLETED

The `PaymentStatus` enum defines five states, but every payment is created as `COMPLETED` and never transitions. The status column and CHECK constraint are scaffolding for future state machine work. Exposing status in the API response was deliberately skipped since it would carry no information.

---

## Validation

Validation is layered and enforced at the earliest possible point:

| Layer | What | How |
|-------|------|-----|
| Frontend form | Amount range, recipient length/pattern, IBAN format | Angular reactive form validators + `ibantools` library |
| Controller boundary | All request fields, pagination params, idempotency key format | Jakarta Bean Validation annotations (`@NotNull`, `@DecimalMin`, `@Size`, `@ValidIban`, `@ValidUuid`) |
| Service layer | Currency existence, amount decimal precision vs. currency config | `PaymentValidationService` throws `InvalidRequestException` |
| Database | Referential integrity, CHECK constraints, unique indexes | FK on currency, CHECK on status/decimals/fee_rate/recipient length |

The `GlobalExceptionHandler` maps every exception type to a structured JSON response (`{ timestamp, status, errors[] }`) and never leaks stack traces or internal details.

---

## Caching

| Cache            | TTL | Max Size | Warmed on startup | Purpose |
|------------------|-----|----------|--------------------|---------|
| `allCurrencies`  | 24h | 500      | Yes                | Caches the full currency list returned by `findAll()` |
| `currencyByCode` | 24h | 500      | No                 | Caches individual currency lookups by code via `findByCode()` |
| `idempotencyKeys`| 24h | 10,000   | No                 | Fast deduplication before hitting the database |

Both caches use Caffeine's `expireAfterWrite` + `maximumSize` (LRU eviction). 

The entire caching subsystem can be disabled via `app.cache.enabled=false`, in which case all lookups fall through to the database.

---

## Testing

**around 400 tests** across 39 test files (~260 backend, ~130 frontend).

| Layer    | Framework                    |
|----------|------------------------------|
| Backend  | JUnit 5, Mockito, H2, MockMvc |
| Frontend | Vitest, Jasmine, HttpTestingController | 

Backend test-to-source line ratio is ~4.2x. 

Coverage reporting is configured via JaCoCo (`jacoco-maven-plugin` 0.8.14). 

Tests use H2 rather than Testcontainers, which is the main fidelity gap.
