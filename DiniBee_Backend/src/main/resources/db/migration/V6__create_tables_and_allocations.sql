-- Required for UUID equality combined with range overlap in the exclusion constraint.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE restaurant_tables (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL REFERENCES restaurants(id),
    table_number varchar(30) NOT NULL,
    table_name varchar(100),
    capacity integer NOT NULL,
    minimum_guests integer NOT NULL,
    maximum_guests integer NOT NULL,
    floor integer NOT NULL,
    section varchar(100),
    shape varchar(30) NOT NULL,
    status varchar(30) NOT NULL,
    x_position numeric(12,2) NOT NULL,
    y_position numeric(12,2) NOT NULL,
    width numeric(12,2) NOT NULL,
    height numeric(12,2) NOT NULL,
    rotation numeric(6,2) NOT NULL,
    active boolean NOT NULL,
    deleted_at timestamptz,
    UNIQUE(id,restaurant_id),
    CHECK (table_number = upper(btrim(table_number)) AND length(table_number)>0),
    CHECK (capacity BETWEEN 1 AND 500 AND minimum_guests BETWEEN 1 AND capacity AND maximum_guests BETWEEN minimum_guests AND capacity),
    CHECK (floor BETWEEN -10 AND 200),
    CHECK (x_position >= 0 AND y_position >= 0 AND width > 0 AND height > 0 AND rotation >= 0 AND rotation < 360),
    CHECK (shape IN ('ROUND','SQUARE','RECTANGLE','OVAL','CUSTOM')),
    CHECK (status IN ('AVAILABLE','RESERVED','OCCUPIED','BLOCKED','MAINTENANCE'))
);

CREATE TABLE table_allocations (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL REFERENCES restaurants(id),
    table_id uuid NOT NULL,
    reference_id uuid NOT NULL,
    reference_type varchar(30) NOT NULL,
    starts_at timestamptz NOT NULL,
    dining_ends_at timestamptz NOT NULL,
    ends_at timestamptz NOT NULL,
    guest_count integer NOT NULL,
    active boolean NOT NULL,
    released_at timestamptz,
    FOREIGN KEY(table_id,restaurant_id) REFERENCES restaurant_tables(id,restaurant_id),
    CHECK (reference_type IN ('RESERVATION','DINITICKET','MANUAL_BLOCK')),
    CHECK (starts_at < dining_ends_at AND dining_ends_at <= ends_at AND guest_count BETWEEN 1 AND 500),
    occupied_period tstzrange GENERATED ALWAYS AS (tstzrange(starts_at,ends_at,'[)')) STORED,
    EXCLUDE USING gist (table_id WITH =, occupied_period WITH &&) WHERE (active)
);

CREATE UNIQUE INDEX restaurant_tables_number_uq ON restaurant_tables(restaurant_id,table_number) WHERE deleted_at IS NULL;

CREATE INDEX restaurant_tables_lookup_idx ON restaurant_tables(restaurant_id,deleted_at,status,active);

CREATE INDEX table_allocations_window_idx ON table_allocations(restaurant_id,starts_at,ends_at) WHERE active;

CREATE UNIQUE INDEX table_allocation_reference_uq ON table_allocations(restaurant_id,reference_type,reference_id,table_id) WHERE active;
