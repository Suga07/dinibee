ALTER TABLE membership_checkout ADD COLUMN IF NOT EXISTS gateway_order_id VARCHAR(100);
ALTER TABLE membership_checkout ADD COLUMN IF NOT EXISTS gateway_payment_id VARCHAR(100);
ALTER TABLE membership_checkout ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50);
ALTER TABLE membership_checkout ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP;
CREATE UNIQUE INDEX IF NOT EXISTS uq_membership_checkout_gateway_order ON membership_checkout(gateway_order_id) WHERE gateway_order_id IS NOT NULL;
