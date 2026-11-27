# Phase 3 upgrade and storage

This package has not passed full Maven/Spring/PostgreSQL verification in the authoring environment. Do not treat it as a certified production release. The phase gate is documented in [VERIFICATION.md](VERIFICATION.md).

## Upgrade an existing installation

Keep the existing database, `com.bookeat` package, JWT secret, token pepper and mail key. V1–V3 are byte-for-byte preserved. V4 adds the restaurant schema; V5 adds permissions and role grants. Flyway validates and applies them on normal startup; Hibernate remains `validate`. Do not enable schema auto-update or repair old checksums to mask a problem.

Back up the database and private configuration. In the extracted project root, run `python scripts/upgrade_phase3_env.py` once for an existing `.env`. It appends only a missing independent settlement key; it never replaces existing secrets. New installations use `scripts/init_env.py` or `scripts/init-env.ps1`, which create all four keys. The setup scripts do not print secrets.

A missing settlement key preserves previous auth startup behavior but settlement writes return 503. The production profile requires a separate valid settlement key and private S3 storage. After adding configuration, run the build and integration gate before deploying.

## Local development

Default private storage is `./private-documents`, outside static web resources. Docker Compose mounts a named `private_documents` volume at `/app/private-documents` owned by the non-root service user. No public static directory is mapped to it. POSIX files/directories get owner-only permissions. On Windows, restrict the storage directory ACL to the service account. Keep this directory out of Git and back it up with the database when its documents need to be retained.

The Docker image build runs `mvn clean package`. PostgreSQL integration tests need a separate host/CI Docker runtime and run with `mvn clean verify -Pintegration`.

## S3 production configuration

```dotenv
SPRING_PROFILES_ACTIVE=prod
DOCUMENT_STORAGE_PROVIDER=S3
DOCUMENT_STORAGE_S3_ENABLED=true
DOCUMENT_STORAGE_S3_BUCKET=your-private-evidence-bucket
DOCUMENT_STORAGE_S3_REGION=ap-south-1
```

Use a private bucket with Block Public Access enabled and a service IAM role restricted to required GetObject/PutObject/DeleteObject operations under the restaurant-document prefix. The adapter uses the AWS SDK default credential chain; it does not embed access keys. Uploads request SSE-S3 encryption and never request public ACLs. Configure the independent settlement key privately in your runtime secret manager.

The SDK is pinned to 2.31.64; this is an explicit available version, not a claim that it is the latest release. The adapter has not been tested against a real S3 bucket here. The selected synchronous URLConnection transport has connection/socket and total API-call timeouts. [AWS SDK Maven setup](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/setup-project-maven.html), [default credential chain](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html), [published S3 dependency](https://central.sonatype.com/artifact/software.amazon.awssdk/s3/2.31.64).

Documents remember which provider stored them. Changing the default from LOCAL to S3 does not migrate existing files; keep the local directory mounted until an explicit, verified copy-and-metadata migration is performed. Removing the old backing storage makes its documents unavailable. No migration is silently attempted.

## Retention and confidentiality

Bank account ciphertext uses AES-256-GCM with random 12-byte IVs, `v1` format and restaurant-specific associated data. Responses contain only the last four digits. The key must be retained and backed up securely; replacing it without a planned data re-encryption migration makes previous ciphertext unreadable. There is no HTTP decrypt endpoint or automated key rotation in this phase.

Evidence upload validates the size and file signature. It is served only as a private binary attachment; a signature check is not malware scanning or proof that the document is genuine. Admin verification records the reviewer’s decision, not an automated regulatory or banking-provider result.

Replacing/deleting evidence soft-deletes the old metadata and preserves the file for audit. Ordinary transaction rollback removes newly uploaded objects on a best-effort basis. A process crash between object upload and database commit can leave an orphan; compare object inventory with database file keys before any controlled retention cleanup. Do not apply a bucket expiry that removes current evidence. A retention/purge policy and provider-specific malware scanning can be integrated before a deployment that requires them.

## Build gate

From the project root with JDK 17, Maven and Docker available:

```bash
mvn clean test
mvn clean package
mvn clean verify -Pintegration
```

The integration profile uses real disposable PostgreSQL containers, not H2. It validates the additive migrations and includes authentication regression plus onboarding isolation, concurrency, permissions, masking and approval behavior. A failure must be fixed before Phase 4 begins. The authored GitHub Actions workflow runs the same gate; no hosted CI run is claimed by this package.
