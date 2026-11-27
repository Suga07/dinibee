CREATE TABLE promotions (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    code varchar(40) NOT NULL,
    title varchar(160) NOT NULL,
    kind varchar(20) NOT NULL,
    restaurant_id uuid,
    service_type varchar(20),
    minimum_amount numeric(18,2) NOT NULL,
    discount_percent numeric(5,2) NOT NULL,
    maximum_discount numeric(18,2) NOT NULL,
    maximum_uses integer NOT NULL,
    uses integer NOT NULL,
    effective_from timestamptz NOT NULL,
    effective_until timestamptz NOT NULL,
    active boolean NOT NULL,
    FOREIGN KEY(restaurant_id) REFERENCES restaurants(id), UNIQUE(code), CHECK(kind IN ('OFFER','COUPON')), CHECK(discount_percent BETWEEN 0 AND 100), CHECK(minimum_amount>=0 AND maximum_discount>=0), CHECK(uses>=0 AND maximum_uses>=uses), CHECK(effective_until>effective_from)
);
CREATE TABLE promotion_redemptions (promotion_id uuid NOT NULL REFERENCES promotions(id), customer_id uuid NOT NULL REFERENCES customers(id), payment_id uuid NOT NULL UNIQUE REFERENCES payments(id), discount_amount numeric(18,2) NOT NULL CHECK(discount_amount>=0), PRIMARY KEY(promotion_id,customer_id));
CREATE TABLE platform_settings (setting_key varchar(100) PRIMARY KEY, setting_value varchar(1000) NOT NULL, updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_by uuid NOT NULL REFERENCES users(id));
