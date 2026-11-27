CREATE TABLE IF NOT EXISTS pay_bills (
 id uuid PRIMARY KEY, restaurant_id uuid NOT NULL REFERENCES restaurants(id),
 customer_id uuid REFERENCES customers(id), booking_id uuid,
 bill_number varchar(90) NOT NULL, source varchar(30) NOT NULL CHECK(source IN ('RESTAURANT','MANUAL')),
 amount_paise bigint NOT NULL CHECK(amount_paise > 0), currency varchar(3) NOT NULL DEFAULT 'INR',
 status varchar(32) NOT NULL CHECK(status IN ('PENDING_APPROVAL','READY_TO_PAY','PAYMENT_PENDING','PAID','REJECTED')),
 verified_by uuid, verified_at timestamptz, rejection_reason varchar(500),
 razorpay_order_id varchar(128) UNIQUE, razorpay_payment_id varchar(128) UNIQUE,
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT pay_bill_number_per_restaurant UNIQUE(restaurant_id,bill_number)
);
CREATE INDEX IF NOT EXISTS pay_bill_customer_lookup ON pay_bills(customer_id,created_at DESC);
CREATE INDEX IF NOT EXISTS pay_bill_restaurant_lookup ON pay_bills(restaurant_id,created_at DESC);
CREATE TABLE IF NOT EXISTS pay_bill_items (
 id uuid PRIMARY KEY, pay_bill_id uuid NOT NULL REFERENCES pay_bills(id), item_name varchar(240) NOT NULL,
 quantity integer NOT NULL CHECK(quantity > 0), unit_price_paise bigint NOT NULL CHECK(unit_price_paise >= 0),
 line_total_paise bigint NOT NULL CHECK(line_total_paise >= 0)
);
CREATE TABLE IF NOT EXISTS pay_bill_payments (
 id uuid PRIMARY KEY, pay_bill_id uuid NOT NULL REFERENCES pay_bills(id),
 razorpay_order_id varchar(128) NOT NULL UNIQUE, razorpay_payment_id varchar(128) UNIQUE,
 amount_paise bigint NOT NULL CHECK(amount_paise > 0), status varchar(30) NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(), verified_at timestamptz
);
