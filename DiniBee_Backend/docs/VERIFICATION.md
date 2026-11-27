# Cumulative Phase 1–15 verification

Checked 2026-09-14. **Source delivery; production acceptance remains pending.**

## Executed successfully

| Command/check | Observed result | Scope |
| --- | --- | --- |
| `python3 scripts/check_phase4_rules.py` | 5,074 assertions passed | Compiled dependency-free production table/menu/availability rules |
| `python3 scripts/check_commerce_rules.py` | 4,093 assertions passed | Compiled dependency-free production commerce rules |
| `python3 scripts/check_final_phase_rules.py` | 23,037 assertions passed | Compiled production dining/payment/wallet rules and CSV/XLSX exporter |
| `python3 scripts/check_zoho_rules.py` | 150 assertions passed | Compiled production Zoho checkout/webhook signatures, tampering, ID and replay boundaries |
| `java scripts/qa/JavaSyntaxCheck.java src` | 427 Java source files parsed | Java 17 syntax only; no dependency type attribution or Lombok processing |
| `java scripts/qa/JavaRecordCheck.java src` | 171 constructor calls checked; 16 ambiguous names skipped | Argument counts only, not type compatibility |
| `python3 scripts/check_final_phase_structure.py` | Passed | Source, contract and preservation checks below |
| YAML/XML parsing | 2 YAML files and POM parsed | Syntax only, not Compose execution or dependency resolution |

Total executed production rule assertions: **32,354**. These are generated invariant/boundary checks, not 32,354 independent end-to-end scenarios. The final-phase runner exercises arrival/radius/state rules, paise conversions and commission rounding, refund limits, known HMAC vectors/tamper cases, wallet balance/reward-debt conservation, CSV formula escaping and five well-formed XML parts inside XLSX. Spreadsheet output was not opened in a desktop Excel application.

The final structure check confirms:

- All applied V1–V21 migrations and `pom.xml` match the delivered Phase 15 SHA-256 baseline (22 protected files).
- V1–V22 migration files are present, ordered without gaps. SQL has not been executed here.
- All 166 Phase 8 Postman request objects are preserved; cumulative collection contains 274 requests and 292 JavaScript event blocks parsed with Node.
- 108 final-phase endpoint literals match controller source; all referenced Postman environment variables exist and JSON request templates parse with typed version substitutions.
- Explicit project imports resolve to source files. Thirty scalar business entities/292 annotated columns match CREATE TABLE or additive ALTER names/nullability. Audit reason's additive ALTER is present. This is not Hibernate schema validation or a complete relational proof.
- Local Markdown file links resolve. JSON files and the Maven XML parse. Full-route literals have no duplicate normalized method/path signature; dynamic Spring mapping and authorization behavior still require the application tests.

## Added and retained test source

The project contains **49 JUnit unit @Test methods** and **145 PostgreSQL integration @Test methods**. The Zoho replacement adds nine unit methods and three integration methods; FinalPhaseFlowIT now contains 37 cases. These JUnit methods were **not executed** in this environment. FinalPhaseFlowIT uses MockMvc/real JWT and Testcontainers PostgreSQL; Mockito replaces only GatewayPort for provider fixtures. Earlier authentication, ownership, onboarding, availability and commerce integration scenarios remain; takeaway/ticket completion scenarios now pay before collection/completion.

New cases cover GPS consent/radius/ownership, expiring/single-use QR, table release and unpaid checkout, server amounts, duplicate payment order/capture, uncertain gateway outcomes and signature mismatch, cash ownership, refund limits/approval, wallet permissions/idempotency, concurrent redemption and refunded spent-reward debt, coupon net payment versus gross obligation, settlement uniqueness, review eligibility/moderation, support privacy/idempotency, notification ownership, blocked-token invalidation, admin role boundaries, report ownership, real dashboard counts and body limits.

## Attempted but unavailable

| Command | Exit | Result |
| --- | --- | --- |
| `mvn clean test` | 127 | NOT EXECUTED — mvn not installed |
| `mvn clean package` | 127 | NOT EXECUTED — mvn not installed |
| `mvn clean verify -Pintegration` | 127 | NOT EXECUTED — mvn not installed |
| `docker --version` | 127 | docker not installed |
| `psql --version` | 127 | psql not installed |

Raw attempt results are in [BUILD_ATTEMPTS.json](BUILD_ATTEMPTS.json); structural evidence is in [STATIC_CHECKS.json](STATIC_CHECKS.json). The actual Java 17 toolchain executed the dependency-free runners and source parser. No full-project compilation, Spring startup, repository SQL, Flyway migration, security filter chain, live provider request, deployed frontend UI, load test or security scan is claimed.

## Release gates

Zoho adapter fixture tests cover major-unit payloads, metadata, fee/tax evidence, ID mismatch, OAuth cache/encoding and one-time POST dispatch. Added database test source covers legacy gateway rejection, account changes and webhook queue/deduplication without provider calls in the HTTP request. These JUnit tests remain unexecuted.

Run the three Maven commands with a compatible JDK and working Docker, resolve any failures, test a restored Phase 8 upgrade, and execute provider/finance/frontend/operations acceptance in [PHASE9_15_DEPLOYMENT.md](PHASE9_15_DEPLOYMENT.md). Provider configuration is disabled by default. Settlement records do not send bank transfers; post-settlement refunds require a funded external adjustment workflow. Actual frontend apps are not modified by this package. Phase 15 production readiness is pending those external gates.

The cumulative ZIP contains MANIFEST.sha256 for every packaged file except the manifest itself. Packaging checks ZIP CRC and each manifest digest; integrity does not substitute for runtime verification.
