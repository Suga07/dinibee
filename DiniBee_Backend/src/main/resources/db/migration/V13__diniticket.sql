CREATE SEQUENCE dini_ticket_sequence;
CREATE TABLE dini_tickets (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    ticket_number varchar(50) NOT NULL,
    queue_sequence bigint NOT NULL,
    restaurant_id uuid NOT NULL,
    customer_id uuid NOT NULL,
    guest_count integer NOT NULL,
    queue_position integer NOT NULL,
    estimated_wait_minutes integer NOT NULL,
    status varchar(30) NOT NULL,
    issued_at timestamptz NOT NULL,
    called_at timestamptz,
    arrived_at timestamptz,
    seated_at timestamptz,
    completed_at timestamptz,
    expires_at timestamptz NOT NULL,
    table_id uuid,
    allocation_id uuid,
    UNIQUE(ticket_number), UNIQUE(queue_sequence), UNIQUE(id,restaurant_id), FOREIGN KEY(customer_id) REFERENCES customers(id), FOREIGN KEY(restaurant_id) REFERENCES restaurants(id), FOREIGN KEY(table_id,restaurant_id) REFERENCES restaurant_tables(id,restaurant_id), FOREIGN KEY(allocation_id) REFERENCES table_allocations(id), CHECK(guest_count BETWEEN 1 AND 500 AND queue_position>=0 AND estimated_wait_minutes>=0), CHECK(status IN ('WAITING','CALLED','ARRIVED','SEATED','COMPLETED','CANCELLED','EXPIRED','NO_SHOW'))
);

CREATE UNIQUE INDEX one_active_ticket_per_customer ON dini_tickets(restaurant_id,customer_id) WHERE status IN ('WAITING','CALLED','ARRIVED','SEATED');
CREATE INDEX dini_ticket_queue_idx ON dini_tickets(restaurant_id,status,queue_sequence);
CREATE INDEX dini_ticket_customer_idx ON dini_tickets(customer_id,status,issued_at DESC);
CREATE INDEX dini_ticket_expiry_idx ON dini_tickets(expires_at,restaurant_id) WHERE status IN ('WAITING','CALLED');
ALTER TABLE food_orders ADD CONSTRAINT food_diniticket_restaurant_fk FOREIGN KEY(dini_ticket_id,restaurant_id) REFERENCES dini_tickets(id,restaurant_id);
CREATE INDEX food_orders_diniticket_idx ON food_orders(dini_ticket_id,status);
CREATE TABLE queue_events (id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, restaurant_id uuid NOT NULL REFERENCES restaurants(id), customer_id uuid NOT NULL REFERENCES customers(id), ticket_id uuid NOT NULL REFERENCES dini_tickets(id), event_type varchar(60) NOT NULL, status varchar(30) NOT NULL, queue_position integer NOT NULL, estimated_wait_minutes integer NOT NULL, created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE INDEX queue_events_owner_idx ON queue_events(restaurant_id,id);
CREATE INDEX queue_events_customer_idx ON queue_events(customer_id,id);
