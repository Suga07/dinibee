-- Additive Pay Bill v2 structures. Requires V63__pay_bills.sql.
CREATE TABLE IF NOT EXISTS pay_bill_splits (
 id uuid PRIMARY KEY, pay_bill_id uuid NOT NULL REFERENCES pay_bills(id),
 customer_id uuid NOT NULL REFERENCES customers(id),
 share_paise bigint NOT NULL CHECK (share_paise > 0),
 status varchar(24) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','PAYMENT_PENDING','PAID')),
 razorpay_order_id varchar(128) UNIQUE, razorpay_payment_id varchar(128) UNIQUE,
 created_at timestamptz NOT NULL DEFAULT now(), paid_at timestamptz,
 UNIQUE(pay_bill_id,customer_id)
);
CREATE INDEX IF NOT EXISTS idx_bill_split_bill ON pay_bill_splits(pay_bill_id);
CREATE TABLE IF NOT EXISTS pay_bill_offers (
 id uuid PRIMARY KEY, restaurant_id uuid REFERENCES restaurants(id),
 code varchar(60) NOT NULL UNIQUE, title varchar(180) NOT NULL,
 offer_type varchar(20) NOT NULL CHECK (offer_type IN ('CASHBACK','DISCOUNT','BANK')),
 premium_only boolean NOT NULL DEFAULT false, active boolean NOT NULL DEFAULT true,
 cashback_percent numeric(6,3) NOT NULL DEFAULT 0 CHECK (cashback_percent BETWEEN 0 AND 100),
 max_cashback_paise bigint NOT NULL DEFAULT 0 CHECK (max_cashback_paise >= 0),
 min_bill_paise bigint NOT NULL DEFAULT 0 CHECK (min_bill_paise >= 0),
 eligible_bank varchar(100), starts_at timestamptz NOT NULL, ends_at timestamptz NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(), CHECK (ends_at > starts_at)
);
CREATE TABLE IF NOT EXISTS pay_bill_adjustments (
 id uuid PRIMARY KEY, pay_bill_id uuid NOT NULL REFERENCES pay_bills(id),
 kind varchar(20) NOT NULL CHECK (kind IN ('REFUND','CASHBACK','SETTLEMENT')),
 amount_paise bigint NOT NULL CHECK (amount_paise > 0),
 status varchar(24) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','PROCESSING','COMPLETED','FAILED')),
 gateway_reference varchar(128), external_reference varchar(180), created_at timestamptz NOT NULL DEFAULT now(),
 updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_bill_adjustment_bill ON pay_bill_adjustments(pay_bill_id);
