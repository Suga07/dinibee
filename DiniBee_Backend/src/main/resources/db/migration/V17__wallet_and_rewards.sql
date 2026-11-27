CREATE TABLE wallets (
    reward_debt bigint NOT NULL DEFAULT 0 CHECK(reward_debt>=0),
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    customer_id uuid NOT NULL,
    balance numeric(18,2) NOT NULL,
    reward_points bigint NOT NULL,
    FOREIGN KEY(customer_id) REFERENCES customers(id), UNIQUE(customer_id), CHECK(balance>=0), CHECK(reward_points>=0)
);
CREATE TABLE wallet_transactions (
    debt_before bigint NOT NULL DEFAULT 0 CHECK(debt_before>=0),
    debt_after bigint NOT NULL DEFAULT 0 CHECK(debt_after>=0),
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    wallet_id uuid NOT NULL,
    transaction_type varchar(30) NOT NULL,
    reference_type varchar(40) NOT NULL,
    reference_id uuid NOT NULL,
    amount numeric(18,2) NOT NULL,
    points bigint NOT NULL,
    balance_before numeric(18,2) NOT NULL,
    balance_after numeric(18,2) NOT NULL,
    points_before bigint NOT NULL,
    points_after bigint NOT NULL,
    reason varchar(1000) NOT NULL,
    actor_id uuid,
    FOREIGN KEY(wallet_id) REFERENCES wallets(id), FOREIGN KEY(actor_id) REFERENCES users(id), UNIQUE(wallet_id,transaction_type,reference_type,reference_id), CHECK(balance_before>=0 AND balance_after>=0 AND points_before>=0 AND points_after>=0)
);
CREATE TABLE reward_rules (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    rule_type varchar(30) NOT NULL,
    minimum_spend numeric(18,2) NOT NULL,
    points bigint NOT NULL,
    conversion_value numeric(18,2) NOT NULL,
    active boolean NOT NULL,
    effective_from timestamptz NOT NULL,
    effective_until timestamptz,
    CHECK(minimum_spend>=0 AND points>=0 AND conversion_value>=0), CHECK(effective_until IS NULL OR effective_until>effective_from)
);
CREATE INDEX wallet_history_idx ON wallet_transactions(wallet_id,created_at DESC,id);
CREATE TABLE reward_claims (customer_id uuid NOT NULL REFERENCES customers(id), rule_type varchar(30) NOT NULL, reference_id uuid NOT NULL, PRIMARY KEY(customer_id,rule_type,reference_id));
CREATE TABLE reward_refund_reversals (refund_id uuid PRIMARY KEY REFERENCES refunds(id),payment_id uuid NOT NULL REFERENCES payments(id),points bigint NOT NULL CHECK(points>=0));
