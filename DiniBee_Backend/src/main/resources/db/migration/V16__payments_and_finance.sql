CREATE TABLE payments (
    discount_amount numeric(18,2) NOT NULL DEFAULT 0 CHECK(discount_amount>=0),
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    customer_id uuid NOT NULL,
    restaurant_id uuid NOT NULL,
    reference_type varchar(30) NOT NULL,
    reference_id uuid NOT NULL,
    gateway varchar(20) NOT NULL,
    gateway_order_id varchar(100),
    gateway_payment_id varchar(100),
    amount numeric(18,2) NOT NULL,
    currency varchar(3) NOT NULL,
    status varchar(30) NOT NULL,
    payment_method varchar(30),
    verified_at timestamptz,
    dispatch_started_at timestamptz,
    failure_reason varchar(200),
    refunded_amount numeric(18,2) NOT NULL,
    commission_amount numeric(18,2) NOT NULL,
    gateway_fee numeric(18,2) NOT NULL,
    gateway_tax numeric(18,2) NOT NULL,
    commission_rule_id uuid,
    settlement_id uuid,
    FOREIGN KEY(customer_id) REFERENCES customers(id), FOREIGN KEY(restaurant_id) REFERENCES restaurants(id), UNIQUE(gateway_order_id), UNIQUE(gateway_payment_id), CHECK(amount>0), CHECK(refunded_amount>=0 AND refunded_amount<=amount), CHECK(commission_amount>=0 AND commission_amount<=amount), CHECK(gateway_fee>=0 AND gateway_tax>=0)
);
CREATE TABLE payment_lines (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    payment_id uuid NOT NULL,
    food_order_id uuid NOT NULL,
    amount numeric(18,2) NOT NULL,
    FOREIGN KEY(payment_id) REFERENCES payments(id), FOREIGN KEY(food_order_id) REFERENCES food_orders(id), UNIQUE(payment_id,food_order_id), CHECK(amount>=0)
);
CREATE TABLE commission_rules (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    service_type varchar(20) NOT NULL,
    commission_type varchar(20) NOT NULL,
    commission_value numeric(18,2) NOT NULL,
    effective_from timestamptz NOT NULL,
    effective_until timestamptz,
    active boolean NOT NULL,
    CHECK(commission_value>=0), CHECK(effective_until IS NULL OR effective_until>effective_from), CHECK(commission_type<>'PERCENTAGE' OR commission_value<=100)
);
CREATE TABLE settlements (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL,
    period_start timestamptz NOT NULL,
    period_end timestamptz NOT NULL,
    gross_amount numeric(18,2) NOT NULL,
    commission_amount numeric(18,2) NOT NULL,
    gateway_fee numeric(18,2) NOT NULL,
    tax_amount numeric(18,2) NOT NULL,
    refund_amount numeric(18,2) NOT NULL,
    net_amount numeric(18,2) NOT NULL,
    status varchar(20) NOT NULL,
    settlement_reference varchar(100),
    processed_at timestamptz,
    FOREIGN KEY(restaurant_id) REFERENCES restaurants(id), CHECK(period_end>period_start), UNIQUE(settlement_reference)
);
CREATE TABLE refunds (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    payment_id uuid NOT NULL,
    amount numeric(18,2) NOT NULL,
    reason varchar(1000) NOT NULL,
    status varchar(20) NOT NULL,
    gateway_refund_id varchar(100),
    requested_by uuid NOT NULL,
    approved_by uuid,
    requested_at timestamptz NOT NULL,
    processed_at timestamptz,
    dispatch_started_at timestamptz,
    failure_reason varchar(200),
    FOREIGN KEY(payment_id) REFERENCES payments(id), FOREIGN KEY(requested_by) REFERENCES users(id), FOREIGN KEY(approved_by) REFERENCES users(id), UNIQUE(gateway_refund_id), CHECK(amount>0)
);
ALTER TABLE payments ADD FOREIGN KEY(commission_rule_id) REFERENCES commission_rules(id);
ALTER TABLE payments ADD FOREIGN KEY(settlement_id) REFERENCES settlements(id);
CREATE INDEX payment_customer_idx ON payments(customer_id,created_at DESC,id);
CREATE INDEX payment_restaurant_idx ON payments(restaurant_id,verified_at);
CREATE TABLE payment_webhook_events (event_id varchar(150) PRIMARY KEY, payload_hash char(64) NOT NULL, received_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP);
