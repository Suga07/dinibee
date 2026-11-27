CREATE TABLE auth_sessions (

    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,

    user_id uuid NOT NULL REFERENCES users(id),
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    last_used_at timestamptz NOT NULL
);
CREATE INDEX auth_sessions_user_idx ON auth_sessions(user_id);
CREATE TABLE refresh_tokens (

    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,

    session_id uuid NOT NULL REFERENCES auth_sessions(id),
    token_hash varchar(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz
);
CREATE INDEX refresh_tokens_session_idx ON refresh_tokens(session_id);
CREATE TABLE auth_challenges (

    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,

    user_id uuid NOT NULL REFERENCES users(id),
    purpose varchar(30) NOT NULL CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    secret_hash varchar(64) NOT NULL,
    expires_at timestamptz NOT NULL,
    attempts integer NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    consumed_at timestamptz
);
CREATE INDEX auth_challenges_user_purpose_idx ON auth_challenges(user_id, purpose, created_at DESC);
CREATE UNIQUE INDEX auth_challenges_one_live_uq ON auth_challenges(user_id, purpose) WHERE consumed_at IS NULL;
CREATE TABLE mail_outbox (

    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,

    recipient varchar(320) NOT NULL,
    subject varchar(160) NOT NULL,
    encrypted_body text,
    status varchar(20) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'EXPIRED')),
    attempts integer NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    sent_at timestamptz
);
CREATE INDEX mail_outbox_pending_idx ON mail_outbox(next_attempt_at) WHERE status = 'PENDING';
CREATE TABLE auth_rate_limits (
    bucket_key varchar(64) PRIMARY KEY,
    window_started_at timestamptz NOT NULL,
    hits integer NOT NULL CHECK (hits >= 0)
);
CREATE INDEX auth_rate_limits_expiry_idx ON auth_rate_limits(window_started_at);
CREATE TABLE audit_logs (

    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,

    actor_id uuid REFERENCES users(id),
    action varchar(80) NOT NULL,
    entity_type varchar(60) NOT NULL,
    entity_id uuid NOT NULL,
    old_value text,
    new_value text,
    ip_address varchar(64)
);
CREATE INDEX audit_logs_entity_idx ON audit_logs(entity_type, entity_id, created_at DESC);
CREATE INDEX audit_logs_actor_idx ON audit_logs(actor_id);
