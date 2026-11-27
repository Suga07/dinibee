CREATE TABLE membership_plans (
    id uuid PRIMARY KEY,
    name varchar(100) NOT NULL,
    description varchar(500),
    price numeric(12,2) NOT NULL CHECK (price >= 0),
    billing_period varchar(20) NOT NULL CHECK (billing_period IN ('MONTHLY','YEARLY','LIFETIME')),
    badge varchar(80),
    benefits text,
    display_order integer NOT NULL DEFAULT 0,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0
);
CREATE INDEX membership_plan_active_idx ON membership_plans(active, display_order, created_at);

CREATE TABLE customer_memberships (
    id uuid PRIMARY KEY,
    customer_id uuid NOT NULL REFERENCES customers(id),
    plan_id uuid NOT NULL REFERENCES membership_plans(id),
    status varchar(20) NOT NULL CHECK (status IN ('ACTIVE','CANCELLED','EXPIRED')),
    starts_at timestamptz NOT NULL,
    ends_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0
);
CREATE INDEX customer_membership_customer_idx ON customer_memberships(customer_id,status,created_at DESC);
