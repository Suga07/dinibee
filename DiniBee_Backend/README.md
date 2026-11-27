# BookEat / DiniBee backend — Phases 1–15

One cumulative Spring Boot backend for customer Flutter, owner Flutter and admin web clients. Phases 9–15 add **dining, payments/finance, wallet/rewards, reviews/support/notifications, administration, reports and frontend integration guidance** to the existing booking/ordering platform.

**Verification is limited:** dependency-free production rule/export checks and Java syntax checks run here; full Maven compilation, JUnit, PostgreSQL and provider acceptance are **NOT EXECUTED**. This is source delivery pending the documented production gates. See [exact verification](docs/VERIFICATION.md).

Start with [Phases 9–15 API contracts](docs/PHASE9_15_API.md), [frontend integration](docs/PHASE14_FRONTEND_INTEGRATION.md), [deployment and acceptance](docs/PHASE9_15_DEPLOYMENT.md), and [change log](docs/PHASE9_15_CHANGELOG.md). The cumulative Postman collection has **274 requests**. V1–V21 and the POM are preserved; V22 adds Zoho account context and webhook jobs. [Zoho Payments](docs/ZOHO_PAYMENTS.md) replaces Razorpay for new online payments and is disabled until configured. Settlement records require independently completed transfers; no automatic bank payout is implemented. Actual frontend apps have not been edited or connected.

## 1. Project choices

| Setting | Value |
| --- | --- |
| Project | Maven / Java / JAR |
| Spring Boot | 3.5.16, stable 3.x release |
| Java | 17 LTS; also builds/runs with a compatible Java 21 JDK |
| Group / package | `com.bookeat` |
| Artifact | `bookeat-backend` |
| Database | PostgreSQL 17 in local Compose/Testcontainers |
| Schema | Flyway migrations; Hibernate `ddl-auto=validate` |
| OpenAPI | springdoc 2.9.1 |
| Phone contact | Optional E.164 phone number stored during registration |
| Authentication | HS256 access JWT, BCrypt cost 12, opaque rotating refresh tokens |

Spring's documentation lists 3.5.16 as a stable 3.x version with Java 17 support. This choice keeps the requested Spring Boot 3 / JUnit 5 stack; it is not presented as the newest Spring Boot major. [Spring Boot requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html)

If creating the project manually in Spring Initializr, select **Spring Web, Spring Data JPA, Spring Security, OAuth2 Resource Server, Validation, PostgreSQL Driver, Flyway Migration, Java Mail Sender, Spring Boot Actuator and Lombok**. The supplied `pom.xml` adds the PostgreSQL Flyway module, OpenAPI and testing dependencies. Do not replace the POM with an Initializr default or choose a snapshot.

## 2. Fastest local setup: Docker Desktop

Extract the ZIP, open a terminal **inside `bookeat-backend`**, and start Docker Desktop. You do not need a separate local Java or Maven installation for this route.

Generate unique private configuration using one of these commands:

```powershell
# Windows PowerShell
.\scripts\init-env.ps1
```

```bash
# Alternatively, with Python 3
python scripts/init_env.py
```

Then start the database, development mail inbox and backend:

```bash
docker compose up --build
```

The first build downloads dependencies and runs the unit tests. If a dependency cannot be downloaded, fix that build error before attempting frontend integration.

| Local page | Purpose |
| --- | --- |
| [Health](http://localhost:8080/api/v1/public/health) | API and PostgreSQL readiness |
| [Swagger](http://localhost:8080/swagger-ui/index.html) | Interactive API documentation in development |
| [Mailpit](http://localhost:8025) | Local verification and reset emails; nothing is delivered to real inboxes |

PostgreSQL uses a named volume, so ordinary container restarts preserve accounts. `docker compose down` stops the services. Deleting the database volume removes the local data; it is not required for routine changes.

## 3. Native Windows / IDE setup

Install a **JDK 17**, Maven 3.6.3+ (3.9.x preferred), and PostgreSQL. Confirm `java -version`, `javac -version` and `mvn -version` in the same terminal. An installed Java runtime alone is insufficient for a Maven build.

Generate `.env` using step 2. Either start only local dependencies:

```bash
docker compose up -d postgres mailpit
```

or create the database manually in `psql` as a PostgreSQL administrator:

```sql
CREATE ROLE bookeat LOGIN;
\password bookeat
CREATE DATABASE bookeat OWNER bookeat ENCODING 'UTF8';
```

The `\password` line is a **psql command**, not SQL for pgAdmin Query Tool. Set the database role password to the private `DB_PASSWORD` value in your `.env`, or update `.env` to match the password you set. For pgAdmin, use its role editor to set the password. Do not recreate an existing database.

Run the backend from PowerShell:

```powershell
.\scripts\run-dev.ps1
```

The script imports `.env` values into the current process and runs `mvn spring-boot:run`. **Spring Boot does not load `.env` automatically.** In IntelliJ, Eclipse or VS Code, configure the same environment variables in the Java run configuration and set the active profile to `dev`.

For Bash, the generated file contains shell-safe values; load it and start Maven:

```bash
set -a
. ./.env
set +a
mvn spring-boot:run
```

If port 8080 is already in use, stop your other backend or set `PORT=8081` in the native run environment and use `http://localhost:8081/api/v1` in Postman. For Compose, change the host mapping to `127.0.0.1:8081:8080`; the container still uses 8080. Do not run native and Compose backends on the same host port at the same time.

## 4. Secrets and configuration

The setup script generates **four different Base64 keys**, each representing 32 random bytes: `JWT_SECRET`, `AUTH_TOKEN_PEPPER`, `MAIL_ENCRYPTION_KEY` and `SETTLEMENT_ENCRYPTION_KEY`. A Base64 string for 32 bytes normally has 44 characters. The script also generates a separate database password. These are server secrets, never Flutter or React configuration.

No production credentials or default admin passwords are included. Keep `.env` out of Git. This project requires generated keys; a descriptive 36-character development phrase from an earlier example will not satisfy this Base64-key format.

| File | Purpose |
| --- | --- |
| `application.properties` | Common database, security, mail and migration settings |
| `application-dev.properties` | Local CORS origins and public Swagger |
| `application-prod.properties` | Explicit production DB, HTTPS origins and authenticated TLS mail |
| `.env.example` | Variable names only; generated values belong in `.env` |

Changing `JWT_SECRET` invalidates existing access-token signatures. Changing the OTP pepper invalidates pending verification challenges. Retain the mail encryption key until pending mail has been sent or expired; this initial design has one active mail key, not a multi-key rotation service.

## 5. First API flow

Import `postman/BookEat_Master.postman_collection.json` and `postman/BookEat_Local.postman_environment.json` into Postman. Select the **BookEat Local** environment and run individual flows.

1. Call `GET {{baseUrl}}/public/health`.
2. Register a customer or owner under `01 Auth`.
3. The Postman test script saves the returned access/refresh token variables.
4. Call `/auth/me` and the appropriate `/customer/profile` or `/owner/profile`.
5. Call `/auth/send-email-otp`, read the code in Mailpit and set `emailOtp`.
6. Call `/auth/verify-email-otp`.
7. Test refresh or logout. Each refresh replaces the stored refresh token; reusing an old one invalidates that session.

Registration example (`POST /api/v1/auth/register/customer`):

```json
{
  "firstName": "Test",
  "lastName": "Customer",
  "email": "customer@example.test",
  "password": "Example-passphrase-71",
  "phoneNumber": "+919876543210"
}
```

Use an email you control when configuring real SMTP. Example credentials are request examples only; no such account is created by migrations. Full endpoint contracts, response examples and error codes are in [API.md](docs/API.md).

## 6. First administrator

Public registration supports only CUSTOMER and OWNER. To create the first administrator, set these variables privately and restart once:

```dotenv
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_EMAIL=your-admin-email
BOOTSTRAP_ADMIN_PASSWORD=your-own-random-password-at-least-20-characters
```

Use a valid email and a password of 20+ characters, within BCrypt's 72 UTF-8 byte limit. Bootstrap runs transactionally with a PostgreSQL advisory lock, refuses to promote an existing public account, and writes an audit event. After the first admin is created, set `BOOTSTRAP_ADMIN_ENABLED=false`, remove the bootstrap password from the runtime configuration and restart. Admins sign in through `/auth/login` like the other clients. Authorized administrators can now create additional admins through the Phase 13 API with existing explicit roles.

## 7. Email and phone verification

Email codes expire in 10 minutes; password-reset tokens expire in 15 minutes. Codes/tokens are single-use. Incorrect verification attempts are committed even when the API returns an error; five wrong attempts consume the challenge. Email resends have a 60-second cooldown.

Forgot-password returns a generic 202 response and queues mail transactionally. The encrypted outbox retries delivery up to five attempts, erases content after delivery/final failure/expiry, and uses PostgreSQL row locking so multiple application instances do not process the same row together. Email delivery can still be duplicated if a process stops after SMTP delivery but before database commit; verification secrets remain single-use.

Phone numbers are optional registration contact data and are stored only after E.164 validation. Phone verification and SMS authentication are not implemented.

This phase uses phone verification to link a verified number to an already signed-in BookEat account. Passwordless phone login is not implemented.

## 8. Run the full tests

```bash
# JUnit unit tests; no database or application secrets needed
mvn clean test

# Package the application after unit tests
mvn clean package

# Complete build + real PostgreSQL/Flyway/security/concurrency tests
# Docker must be running for Testcontainers
mvn clean verify -Pintegration
```

The integration profile fails if Docker is unavailable; it does not silently substitute H2 or skip PostgreSQL behavior. It creates a disposable PostgreSQL container and does not use your development database. The included GitHub Actions workflow runs this profile. It has been authored but not executed here.

## 9. Frontend integration

Use [FRONTEND_INTEGRATION.md](docs/FRONTEND_INTEGRATION.md) for the customer, owner and admin clients. Keep existing business-page mock data until its real endpoints exist and are verified.

Phase 3 endpoints and example payloads are documented in [PHASE3_API.md](docs/PHASE3_API.md). Owners request services; admins verify evidence and approve an explicit subset. Flutter should display modules from `enabledServices` in `/owner/dashboard-context`.

On an existing Phase 1/2 installation, keep the database and existing secrets. Run `python scripts/upgrade_phase3_env.py` once (Python 3 required) to add the independent settlement key, or add a separately generated 32-byte Base64 key privately to the existing environment. Restart normally: Flyway applies pending migrations V4–V8 as appropriate. Do not regenerate existing keys or reset the database. New installations use the normal init-env script.

Run `mvn clean test`, then `mvn clean package`, then `mvn clean verify -Pintegration` with Docker running. Resolve all build/test failures before live client integration or deployment. See [Phase 3 deployment notes](docs/PHASE3_DEPLOYMENT.md) and [Phase 4 upgrade notes](docs/PHASE4_DEPLOYMENT.md).

Additional documentation: [module guide](docs/MODULES.md), [source references](docs/SOURCES.md), [verification report](docs/VERIFICATION.md), [deployment notes](docs/DEPLOYMENT.md).

## 10. Phase 4 table/menu flow

Read [PHASE4_API.md](docs/PHASE4_API.md). In Postman, get the restaurant detail to save restaurantVersion; use folders 04 Tables and 05 Menu to create inventory. Item requests contain variants/addons. Owner mutations return the new parent restaurantVersion; keep it for the next write. Category/item/table IDs are always scoped to the owned restaurant.

After approval and RESTAURANT service activation, configure a date/time/guest count in folder 06 to query public availability. A fitting active AVAILABLE table, reservation settings and operating hours must exist. Results include offset timestamps and protect dining duration plus grace; a read does not hold a table. Public menu/search and transactional booking creation are now included in Phases 5–6. See [the current API guide](docs/PHASE5_8_API.md).

The new database migration needs PostgreSQL's btree_gist extension. Follow [the Phase 4 upgrade notes](docs/PHASE4_DEPLOYMENT.md) for existing databases. No new secrets or dependencies are added by Phase 4.
