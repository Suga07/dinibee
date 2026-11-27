CREATE TABLE dining_sessions (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    reservation_id uuid,
    dini_ticket_id uuid,
    customer_id uuid NOT NULL,
    restaurant_id uuid NOT NULL,
    table_id uuid,
    status varchar(30) NOT NULL,
    check_in_method varchar(30) NOT NULL,
    check_in_latitude numeric(10,7),
    check_in_longitude numeric(10,7),
    checked_in_at timestamptz NOT NULL,
    dining_started_at timestamptz,
    payment_pending_at timestamptz,
    paid_at timestamptz,
    checked_out_at timestamptz,
    bill_total numeric(18,2) NOT NULL,
    FOREIGN KEY(reservation_id) REFERENCES reservations(id), FOREIGN KEY(dini_ticket_id) REFERENCES dini_tickets(id), FOREIGN KEY(customer_id) REFERENCES customers(id), FOREIGN KEY(restaurant_id) REFERENCES restaurants(id), FOREIGN KEY(table_id) REFERENCES restaurant_tables(id), UNIQUE(reservation_id), UNIQUE(dini_ticket_id), CHECK((reservation_id IS NULL)<>(dini_ticket_id IS NULL)), CHECK(bill_total>=0)
);
CREATE TABLE dining_checkin_tokens (reservation_id uuid PRIMARY KEY REFERENCES reservations(id), token_hash char(64) NOT NULL UNIQUE, expires_at timestamptz NOT NULL, consumed_at timestamptz);
CREATE INDEX dining_current_idx ON dining_sessions(customer_id,status);
CREATE INDEX dining_restaurant_idx ON dining_sessions(restaurant_id,created_at DESC,id);
