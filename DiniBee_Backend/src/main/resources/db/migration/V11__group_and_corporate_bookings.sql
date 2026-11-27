CREATE TABLE group_bookings (
    change_sequence bigint NOT NULL DEFAULT 0,
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    reservation_id uuid NOT NULL,
    host_customer_id uuid NOT NULL,
    restaurant_id uuid NOT NULL,
    invite_hash varchar(64),
    invite_expires_at timestamptz,
    member_limit integer NOT NULL,
    status varchar(30) NOT NULL,
    payment_mode varchar(30) NOT NULL,
    UNIQUE(reservation_id), UNIQUE(invite_hash), UNIQUE(id,restaurant_id), FOREIGN KEY(reservation_id,restaurant_id) REFERENCES reservations(id,restaurant_id), FOREIGN KEY(host_customer_id) REFERENCES customers(id), CHECK(member_limit BETWEEN 1 AND 500), CHECK(status IN ('CREATED','ACTIVE','LOCKED','COMPLETED','CANCELLED')), CHECK(payment_mode IN ('HOST_PAYS','SPLIT_EQUAL','SPLIT_BY_ITEMS'))
);
CREATE TABLE group_members (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    group_booking_id uuid NOT NULL,
    customer_id uuid NOT NULL,
    display_name varchar(100) NOT NULL,
    member_status varchar(30) NOT NULL,
    joined_at timestamptz,
    UNIQUE(group_booking_id,customer_id), FOREIGN KEY(group_booking_id) REFERENCES group_bookings(id), FOREIGN KEY(customer_id) REFERENCES customers(id), CHECK(member_status IN ('INVITED','JOINED','LEFT','REMOVED'))
);
CREATE TABLE corporate_accounts (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    owner_customer_id uuid NOT NULL,
    company_name varchar(160) NOT NULL,
    gst_number varchar(30),
    billing_address varchar(1000) NOT NULL,
    contact_person_name varchar(100) NOT NULL,
    contact_email varchar(320) NOT NULL,
    contact_phone varchar(30) NOT NULL,
    active boolean NOT NULL,
    FOREIGN KEY(owner_customer_id) REFERENCES customers(id)
);
CREATE TABLE corporate_bookings (
    change_sequence bigint NOT NULL DEFAULT 0,
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    corporate_account_id uuid NOT NULL,
    reservation_id uuid NOT NULL,
    restaurant_id uuid NOT NULL,
    organizer_customer_id uuid NOT NULL,
    series_id uuid NOT NULL,
    budget_limit numeric(18,2) NOT NULL,
    billing_type varchar(30) NOT NULL,
    recurrence_type varchar(30) NOT NULL,
    recurrence_rule varchar(300) NOT NULL,
    invoice_required boolean NOT NULL,
    UNIQUE(reservation_id), UNIQUE(id,restaurant_id), FOREIGN KEY(corporate_account_id) REFERENCES corporate_accounts(id), FOREIGN KEY(reservation_id,restaurant_id) REFERENCES reservations(id,restaurant_id), FOREIGN KEY(organizer_customer_id) REFERENCES customers(id), CHECK(budget_limit>=0), CHECK(billing_type IN ('COMPANY','INDIVIDUAL')), CHECK(recurrence_type IN ('NONE','DAILY','WEEKLY','MONTHLY'))
);
CREATE TABLE corporate_guests (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    corporate_booking_id uuid NOT NULL,
    customer_id uuid,
    name varchar(100) NOT NULL,
    email varchar(320) NOT NULL,
    phone varchar(30),
    employee_code varchar(80),
    status varchar(30) NOT NULL,
    invite_hash varchar(64) NOT NULL,
    invite_expires_at timestamptz NOT NULL,
    UNIQUE(corporate_booking_id,email), UNIQUE(invite_hash), FOREIGN KEY(corporate_booking_id) REFERENCES corporate_bookings(id), FOREIGN KEY(customer_id) REFERENCES customers(id), CHECK(status IN ('INVITED','JOINED','REMOVED'))
);
CREATE INDEX corporate_bookings_customer_idx ON corporate_bookings(organizer_customer_id,created_at DESC,id);
CREATE INDEX corporate_series_idx ON corporate_bookings(series_id,id);
CREATE UNIQUE INDEX corporate_guest_customer_uq ON corporate_guests(corporate_booking_id,customer_id) WHERE customer_id IS NOT NULL;
