CREATE TABLE IF NOT EXISTS diniticket_sessions (
    id uuid PRIMARY KEY,
    restaurant_id uuid NOT NULL REFERENCES restaurants(id),
    started_at timestamptz NOT NULL DEFAULT now(),
    ended_at timestamptz NULL,
    max_tokens integer NOT NULL DEFAULT 100 CHECK (max_tokens > 0),
    issued_count integer NOT NULL DEFAULT 0 CHECK (issued_count >= 0),
    active boolean NOT NULL DEFAULT true
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_diniticket_active_session_per_restaurant
    ON diniticket_sessions(restaurant_id) WHERE active = true;
CREATE INDEX IF NOT EXISTS idx_diniticket_sessions_restaurant_started
    ON diniticket_sessions(restaurant_id, started_at DESC);

-- Token labels (DT-1, DT-2, ...) are session-local. The globally unique/FIFO
-- identity remains dini_tickets.id + queue_sequence.
ALTER TABLE dini_tickets DROP CONSTRAINT IF EXISTS dini_tickets_ticket_number_key;
