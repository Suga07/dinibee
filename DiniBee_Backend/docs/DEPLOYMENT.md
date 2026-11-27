# Deployment preparation

> Current cumulative delivery: Phases 1–15. See [current API](PHASE9_15_API.md), [Phase 14 integration](PHASE14_FRONTEND_INTEGRATION.md), [deployment gates](PHASE9_15_DEPLOYMENT.md) and [verification](VERIFICATION.md). Earlier sections retain historical phase context.




No remote infrastructure has been created and no deployment has been performed. The included Compose file is a **local development setup**, with Mailpit and one PostgreSQL instance.

## Verification gate

Run `mvn clean verify -Pintegration` in a network-enabled environment with JDK 17, Maven and Docker. Resolve dependency, schema and test failures before releasing. The first Docker build also runs unit tests. Check the chosen dependency release lines and container images for current maintenance/security updates, and pin reviewed image digests for a release.

## AWS-oriented runtime

The Docker image runs as a non-root user. It can be deployed to a container runtime such as ECS with PostgreSQL in RDS, an HTTPS load balancer, and secrets injected at runtime. This is deployment guidance, not a claim that ECS/RDS resources or Terraform are included.

Set `SPRING_PROFILES_ACTIVE=prod`, DB variables, the three independent keys, an exact HTTPS admin CORS origin, and authenticated SMTP settings. Use a PostgreSQL connection configuration with TLS certificate verification for the deployed database. Configure the trusted database CA path if your driver needs it; do not remove verification merely to suppress an SSL error.

Prefer a dedicated migration job/role with schema permissions and a restricted runtime DB role. For that model, run the exact Flyway migrations as the release gate, disable in-app migration with `SPRING_FLYWAY_ENABLED=false` for runtime containers, and retain Hibernate schema validation. Do not enable `ddl-auto=create/update` on production data.

The API should be reachable through HTTPS. PostgreSQL and SMTP credentials remain private. Configure health/readiness checks, database backups and tested restores, and alerts for failed mail entries and authentication failures. `GET /api/v1/public/health` checks the database; Actuator exposes only health and omits details.

## Reverse proxy and rate limits

The application deliberately ignores arbitrary forwarded headers. Behind a load balancer, direct peer IPs can collapse onto a few proxy addresses; deploy edge request limits and configure trusted forwarding deliberately before real traffic. Account-keyed limits still apply. Do not enable unrestricted trust of client-supplied forwarding headers.

## Session and mail retention

Consumed refresh tokens must remain until their session is no longer usable so reuse can be detected. Define a retention job for expired sessions/tokens/challenges and completed outbox metadata; no arbitrary account/payment history deletion is implemented. Erasing challenge rows must never make a revoked session active again.

This version uses one active JWT signing secret and one active mail encryption key. For planned key rotation without disrupting valid sessions or pending emails, add a reviewed key-ID/keyring process. Immediately replacing the signing secret invalidates old access signatures; rotate credentials deliberately.

## External services still to validate

Real SMTP account linking requires provider configuration and end-to-end verification. Payment/Razorpay, S3 uploads, restaurant settlement and notification providers belong to later modules and have not been activated.

Phase 3 additions: [upgrade, private storage and settlement key](PHASE3_DEPLOYMENT.md).

## Cumulative Phase 4 upgrade

Follow [PHASE4_DEPLOYMENT.md](PHASE4_DEPLOYMENT.md) for V6–V8 and btree_gist requirements. Keep existing keys and data. Run the cumulative Maven/Testcontainers gates before production deployment.
