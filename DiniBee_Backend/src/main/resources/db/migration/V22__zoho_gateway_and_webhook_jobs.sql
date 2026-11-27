-- Preserve existing provider labels and financial history; never relabel old transactions.
ALTER TABLE payments ADD COLUMN gateway_account_id varchar(32);
ALTER TABLE payments ADD COLUMN gateway_sandbox boolean;
ALTER TABLE payments ADD CONSTRAINT zoho_account_context CHECK(gateway <> 'ZOHO' OR (gateway_account_id IS NOT NULL AND gateway_sandbox IS NOT NULL));
CREATE TABLE zoho_webhook_jobs (
    event_id varchar(150) PRIMARY KEY REFERENCES payment_webhook_events(event_id),
    event_type varchar(40) NOT NULL,
    resource_id varchar(32) NOT NULL,
    account_id varchar(32) NOT NULL,
    sandbox boolean NOT NULL,
    attempts integer NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lease_id uuid,
    lease_until timestamptz,
    completed_at timestamptz,
    last_error varchar(200),
    CHECK(attempts >= 0)
);
CREATE INDEX zoho_webhook_work_idx ON zoho_webhook_jobs(next_attempt_at) WHERE completed_at IS NULL;
