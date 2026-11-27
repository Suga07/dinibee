CREATE TABLE membership_checkout (
    id uuid PRIMARY KEY,
    customer_id uuid NOT NULL REFERENCES customers(id),
    plan_id uuid NOT NULL REFERENCES membership_plans(id),
    amount numeric(12,2) NOT NULL CHECK (amount >= 0),
    status varchar(20) NOT NULL CHECK (status IN ('PENDING','PAID','CANCELLED','FAILED')),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at timestamptz
);
CREATE INDEX membership_checkout_customer_idx ON membership_checkout(customer_id,status,created_at DESC);
