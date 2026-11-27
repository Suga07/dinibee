CREATE TABLE menu_categories (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL REFERENCES restaurants(id),
    name varchar(100) NOT NULL,
    description varchar(2000),
    display_order integer NOT NULL,
    active boolean NOT NULL,
    deleted_at timestamptz,
    UNIQUE(id,restaurant_id),
    CHECK (length(btrim(name)) > 0 AND display_order >= 0)
);

CREATE TABLE menu_items (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL REFERENCES restaurants(id),
    category_id uuid NOT NULL,
    name varchar(160) NOT NULL,
    description varchar(3000),
    base_price numeric(12,2) NOT NULL,
    discount_price numeric(12,2),
    food_type varchar(30) NOT NULL,
    spice_level varchar(30) NOT NULL,
    image_url varchar(2048),
    available boolean NOT NULL,
    pre_order_available boolean NOT NULL,
    takeaway_available boolean NOT NULL,
    dini_ticket_pre_order_available boolean NOT NULL,
    preparation_time_minutes integer NOT NULL,
    featured boolean NOT NULL,
    display_order integer NOT NULL,
    deleted_at timestamptz,
    FOREIGN KEY(category_id,restaurant_id) REFERENCES menu_categories(id,restaurant_id),
    CHECK (base_price >= 0 AND (discount_price IS NULL OR (discount_price >= 0 AND discount_price <= base_price))),
    CHECK (food_type IN ('VEG','NON_VEG','VEGAN','EGG')),
    CHECK (spice_level IN ('NONE','MILD','MEDIUM','HOT','EXTRA_HOT')),
    CHECK (length(btrim(name))>0 AND preparation_time_minutes BETWEEN 0 AND 1440 AND display_order>=0)
);

CREATE TABLE menu_variants (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    menu_item_id uuid NOT NULL REFERENCES menu_items(id),
    name varchar(80) NOT NULL,
    price numeric(12,2) NOT NULL,
    active boolean NOT NULL,
    deleted_at timestamptz,
    CHECK (price >= 0 AND length(btrim(name))>0)
);

CREATE TABLE menu_addons (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    menu_item_id uuid NOT NULL REFERENCES menu_items(id),
    name varchar(80) NOT NULL,
    price numeric(12,2) NOT NULL,
    active boolean NOT NULL,
    deleted_at timestamptz,
    CHECK (price >= 0 AND length(btrim(name))>0)
);

CREATE UNIQUE INDEX menu_categories_name_uq ON menu_categories(restaurant_id,lower(btrim(name))) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX menu_items_name_uq ON menu_items(restaurant_id,category_id,lower(btrim(name))) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX menu_variants_name_uq ON menu_variants(menu_item_id,lower(btrim(name))) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX menu_addons_name_uq ON menu_addons(menu_item_id,lower(btrim(name))) WHERE deleted_at IS NULL;

CREATE INDEX menu_items_listing_idx ON menu_items(restaurant_id,deleted_at,display_order,id);

CREATE INDEX menu_categories_listing_idx ON menu_categories(restaurant_id,deleted_at,display_order,id);
