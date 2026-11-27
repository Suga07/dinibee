CREATE TABLE reservations (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    booking_reference varchar(40) NOT NULL,
    customer_id uuid NOT NULL,
    restaurant_id uuid NOT NULL,
    table_id uuid NOT NULL,
    allocation_id uuid,
    reservation_date date NOT NULL,
    start_time time NOT NULL,
    end_time time NOT NULL,
    starts_at timestamptz NOT NULL,
    ends_at timestamptz NOT NULL,
    timezone varchar(60) NOT NULL,
    guest_count integer NOT NULL,
    special_request varchar(2000),
    booking_type varchar(30) NOT NULL,
    status varchar(30) NOT NULL,
    payment_status varchar(30) NOT NULL,
    cancellation_allowed boolean NOT NULL,
    cancellation_deadline timestamptz NOT NULL,
    no_show_after timestamptz NOT NULL,
    checked_in_at timestamptz,
    completed_at timestamptz,
    cancelled_at timestamptz,
    cancellation_reason varchar(1000),
    UNIQUE(booking_reference), UNIQUE(id,restaurant_id), FOREIGN KEY(customer_id) REFERENCES customers(id), FOREIGN KEY(restaurant_id) REFERENCES restaurants(id), FOREIGN KEY(table_id,restaurant_id) REFERENCES restaurant_tables(id,restaurant_id), FOREIGN KEY(allocation_id) REFERENCES table_allocations(id), CHECK(starts_at<ends_at AND guest_count BETWEEN 1 AND 500), CHECK(booking_type IN ('REGULAR','GROUP','CORPORATE')), CHECK(status IN ('PENDING','CONFIRMED','ARRIVED','CHECKED_IN','DINING','COMPLETED','CANCELLED','NO_SHOW','REJECTED')), CHECK(payment_status IN ('UNPAID','PAID','PARTIALLY_PAID','REFUNDED'))
);
CREATE TABLE reservation_status_history (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    reservation_id uuid NOT NULL,
    old_status varchar(30),
    new_status varchar(30) NOT NULL,
    changed_by uuid NOT NULL,
    reason varchar(1000),
    changed_at timestamptz NOT NULL,
    FOREIGN KEY(reservation_id) REFERENCES reservations(id), FOREIGN KEY(changed_by) REFERENCES users(id)
);
CREATE INDEX reservations_customer_idx ON reservations(customer_id,starts_at DESC,id);
CREATE INDEX reservations_owner_idx ON reservations(restaurant_id,starts_at,status,id);
CREATE INDEX reservation_history_idx ON reservation_status_history(reservation_id,changed_at,id);
