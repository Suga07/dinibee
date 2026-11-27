# Version and integration references

Checked while preparing this package on 11 September 2026. These pages ground dependency choices and provider behavior; they do not prove that this generated project passed a Maven build.

- [Spring Boot 3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html): stable 3.5.16, Java 17 baseline, supported Maven versions.
- [springdoc 2.x documentation](https://springdoc.org/v2/): Spring Boot 3 integration and the 2.9.1 dependency.
- [Spring Security 6.5 JWT resource server](https://docs.spring.io/spring-security/reference/6.5/servlet/oauth2/resource-server/jwt.html): JWT decoder/encoder and resource-server extension points.

## Phase 3 storage references

- [AWS Java SDK Maven setup](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/setup-project-maven.html) — S3 module and synchronous transport integration.
- [AWS default credential provider chain](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html) — runtime IAM credentials.
- [S3 SDK 2.31.64 published artifact](https://central.sonatype.com/artifact/software.amazon.awssdk/s3/2.31.64) — explicit dependency pin, not a latest-version claim.

## Phase 4 references

Consulted 2026-09-12 for range exclusion semantics, UUID GiST support and timezone offsets. These references support the design; they do not verify this application's compilation or migrations.

- [PostgreSQL 17 range types and constraints](https://www.postgresql.org/docs/17/rangetypes.html): half-open intervals and exclusion-based overlap protection.
- [PostgreSQL 17 btree_gist](https://www.postgresql.org/docs/17/btree-gist.html): UUID equality support for multicolumn GiST constraints and extension installation.
- [Java 17 ZoneRules](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/zone/ZoneRules.html): valid local-time offsets, gaps and overlaps.

## Phase 5–8 references

Consulted 2026-09-13. These support database semantics, not proof that these application migrations executed. Dependency pins from earlier phases are unchanged.

- [PostgreSQL 17 explicit locking](https://www.postgresql.org/docs/17/explicit-locking.html): row locks, lock ordering and transaction-scoped advisory locks.
- [PostgreSQL 17 date/time functions](https://www.postgresql.org/docs/17/functions-datetime.html): timestamp/date arithmetic and AT TIME ZONE used by public opening windows.

## Phase 10 provider references — Zoho replacement

Checked 14 September 2026 against official Zoho India API, developer documentation and SDK environment mappings. [ZOHO_PAYMENTS.md](ZOHO_PAYMENTS.md) links each primary source beside its supported contract. Earlier Razorpay references no longer describe the active adapter. Provider account integration has not been executed; confirm checkout, fee/tax, refunds and webhook retries in sandbox. No new Maven dependencies were introduced.
