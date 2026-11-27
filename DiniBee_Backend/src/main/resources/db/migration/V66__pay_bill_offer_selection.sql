-- Store selected offers, verified against live restaurant and customer eligibility.
-- No money movement occurs from a selection alone.
CREATE TABLE IF NOT EXISTS pay_bill_offer_selections (
  pay_bill_id uuid PRIMARY KEY REFERENCES pay_bills(id),
  offer_id uuid NOT NULL REFERENCES pay_bill_offers(id),
  selected_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_bill_offer_selection_offer ON pay_bill_offer_selections(offer_id);
