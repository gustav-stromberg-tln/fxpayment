# FX Payment Processor

Foreign exchange payment processing service with multi-currency support, automatic fee calculation, IBAN validation, and idempotent payment creation.

For a more in-depth look at the system design, see [doc/architecture-overview.md](doc/architecture-overview.md).

## Tech Stack

- **Backend:** Java 25, Spring Boot 3.5, PostgreSQL 18, Flyway, Caffeine cache
- **Frontend:** Angular 21, TypeScript 5.9, Bootstrap 5.3, RxJS
- **Infra:** Docker, Docker Compose, Nginx

## Quick Start

```bash
docker compose up
```

- Frontend: http://localhost:4200
- API: http://localhost:8080

The `docker-compose.yml` ships with sensible defaults already hardcoded, so no `.env` file is needed to get started:

| Variable | Default |
|----------|---------|
| `POSTGRES_DB` | `fxpayment` |
| `POSTGRES_USER` | `fxuser` |
| `POSTGRES_PASSWORD` | `changeme` |

These defaults are used for the database service, healthcheck, and backend connection (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`). Override any of them by setting the corresponding environment variable before running `docker compose up`.

## Local Setup (without Docker)

### 1. Database

Install and start PostgreSQL 18, then create the database and user:

```bash
psql -U postgres -c "CREATE USER fxuser WITH PASSWORD 'changeme';"
psql -U postgres -c "CREATE DATABASE fxpayment OWNER fxuser;"
```
> **Note:** On Linux with default peer authentication, you may need to run the commands as the `postgres` OS user instead: `sudo -u postgres psql -c "..."`

Flyway runs migrations automatically when the backend starts (with retry: 5 attempts, 2s interval).

### 2. Backend

**Prerequisites:** Java 25 JDK, Maven 3.9+

```bash
cd backend

export DB_URL=jdbc:postgresql://localhost:5432/fxpayment
export DB_USERNAME=fxuser
export DB_PASSWORD=changeme

mvn spring-boot:run
```

Or build and run the JAR (the same environment variables above must be set):

```bash
mvn clean package -DskipTests
java -jar target/backend-1.0-SNAPSHOT.jar
```

The backend starts on http://localhost:8080 with virtual threads enabled.

### 3. Frontend

**Prerequisites:** Node.js 22+, npm

```bash
cd frontend
npm install
npm start
```

The dev server starts on http://localhost:4200 and proxies `/api` requests to `http://localhost:8080` via `proxy.conf.json`.

## Running Tests

### Backend

```bash
cd backend
mvn test
```

Uses JUnit 5, Mockito, and H2 in-memory database. Includes tests.

### Frontend

```bash
cd frontend
npm test              # single run (default, avoids esbuild deadlock on exit)
npm run test:watch    # watch mode for interactive development
```

Uses Vitest with Jasmine-style assertions and `HttpTestingController` for HTTP mocking.

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/payments` | Create a payment (idempotent) |
| `GET` | `/api/v1/payments?page=0&size=20` | List payments (paginated, sorted by `createdAt DESC`) |
| `GET` | `/api/v1/currencies` | List supported currencies |

### Creating a payment

```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{
    "amount": 100.00,
    "currency": "USD",
    "recipient": "John Smith",
    "recipientAccount": "DE89370400440532013000"
  }'
```

The `Idempotency-Key` header is **required** and must be a valid UUID. Submitting the same key twice returns the original response with `200 OK` instead of creating a duplicate.

### Pagination

`GET /api/v1/payments` accepts `page` (default 0) and `size` (default 20, max 100) query parameters. Results are sorted by creation time descending.

### Payment status

Payment status (`COMPLETED`, `PENDING`, etc.) is stored in the database but intentionally excluded from the API response. All payments are created as `COMPLETED` and the status never changes, so exposing it would add no value. State transitions are a draft for future work.

## Fee Calculation

Processing fees are calculated on the backend by `FeeCalculationService`:

- **EUR:** No fee (0%)
- **USD / GBP:** 1% of the transaction amount, minimum 5.00 units in the respective currency

Formula: `fee = max(amount * feeRate, minimumFee)`

| Example | Amount | 1% | Minimum | Applied Fee |
|---------|--------|-----|---------|-------------|
| USD 100 | 100.00 | 1.00 | 5.00 | **5.00** (minimum applies) |
| USD 501 | 501.00 | 5.01 | 5.00 | **5.01** (1% exceeds minimum) |
| EUR 1000 | 1000.00 | 0.00 | 0.00 | **0.00** |

Fees are stored in the `processing_fee` column as `NUMERIC(19,4)`. Internal calculations use BigDecimal with scale 4 and `HALF_UP` rounding. 

API responses round to the currency's display decimals.

## Supported Currencies

| Code | Name | Decimals | Fee Rate | Min Fee |
|------|------|----------|----------|---------|
| EUR | Euro | 2 | 0% | 0.00 |
| USD | US Dollar | 2 | 1% | 5.00 |
| GBP | British Pound | 2 | 1% | 5.00 |

Currencies are stored in the database and served via `/api/v1/currencies`. 

The `decimals` column follows ISO 4217 minor unit precision. 

Input validation rejects amounts exceeding a currency's decimal precision (e.g., `100.123` USD would be invalid since USD allows only 2 decimal places).

## Project Structure

```
backend/
  src/main/java/com/fxpayment/
    controller/    # REST endpoints (PaymentController, CurrencyController)
    service/       # Business logic (fees, payments, validation, caching)
    model/         # JPA entities (Payment, CurrencyEntity, PaymentStatus)
    dto/           # Request/response records
    repository/    # Spring Data JPA repositories
    config/        # CORS, cache, exception handling, cache warming, request correlation, startup logging
    exception/     # Custom exceptions
    validation/    # Custom validators (@ValidIban, @ValidUuid)
    util/          # BigDecimal rounding utilities, constants
  src/main/resources/
    application.yml
    db/migration/  # Flyway SQL migrations

frontend/
  src/app/
    components/    # PaymentFormComponent, PaymentHistoryComponent
    services/      # PaymentService, CurrencyService, NotificationService
    models/        # TypeScript interfaces for API types
    interceptors/  # Global HTTP error interceptor
    handlers/      # Global runtime error handler
    validators/    # IBAN validator
    pipes/         # Currency-aware amount formatting
```

## Configuration

Key settings in `backend/src/main/resources/application.yml`:

| Setting | Default | Description |
|---------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://db:5432/fxpayment` | Database connection URL |
| `DB_USERNAME` / `DB_PASSWORD` | (required) | Database credentials via env vars |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Allowed CORS origins |
| `CORS_ALLOWED_METHODS` | `GET,POST` | Allowed HTTP methods |
| `CORS_ALLOWED_HEADERS` | `Content-Type,Idempotency-Key,X-Request-Id` | Allowed request headers |
| Virtual threads | enabled | Spring Boot dispatches requests on virtual threads |
| Currency cache TTL | 24h (max 500 entries) | Caffeine in-memory cache for currency data |
| Idempotency cache TTL | 24h (max 10,000 entries) | Caffeine cache for deduplication |

CORS is configured via `WebConfig` (a `WebMvcConfigurer` bean) with settings bound from `application.yml`. All values are overridable via environment variables.

## Operational Endpoints

Spring Boot Actuator provides health, info, and metrics endpoints:

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Returns `{"status":"UP"}` when the application is healthy |
| `GET /actuator/info` | Application metadata |
| `GET /actuator/metrics` | JVM, HTTP, and Spring-managed metrics |

### Request Correlation

Every response includes an `X-Request-Id` header. If the client sends this header, the value is echoed back; otherwise a UUID is generated. The same ID appears in all server-side log lines for that request via SLF4J MDC.

## Security

This is a test project scoped to a coding exercise, not a production deployment.

### Intentionally omitted

| Concern | Rationale |
|---------|-----------|
| TLS/HTTPS | Infrastructure concern, not relevant to the task |
| Authentication (JWT/OAuth) | No user model or auth requirement in scope |
| Rate limiting | Operational concern, out of scope |
| Per-user data scoping | Requires authentication first |

Spring Security is not included because there is no authentication model, no user roles, and no session state. Adding it would mean immediately disabling most defaults to keep the API working.

### Implemented

- **Input validation:** Jakarta Bean Validation on all request fields, including custom IBAN validator (Apache Commons Validator + MOD-97 check-digit)
- **Idempotency:** Client-generated UUID keys prevent duplicate payment processing
- **SQL injection prevention:** All data access through JPA/Hibernate parameterised queries
- **Error handling:** `GlobalExceptionHandler` returns structured `{ timestamp, status, errors[] }` responses without leaking stack traces
- **Credential management:** Database credentials externalized via environment variables
- **Schema integrity:** Flyway migrations with `ddl-auto: validate`
