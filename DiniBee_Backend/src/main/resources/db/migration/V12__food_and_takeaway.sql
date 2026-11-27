CREATE TABLE restaurant_order_settings (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL,
    tax_percent numeric(5,2) NOT NULL,
    UNIQUE(restaurant_id), FOREIGN KEY(restaurant_id) REFERENCES restaurants(id), CHECK(tax_percent BETWEEN 0 AND 100)
);
CREATE TABLE food_orders (
    tax_percent numeric(5,2) NOT NULL DEFAULT 0 CHECK(tax_percent BETWEEN 0 AND 100),
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    order_number varchar(40) NOT NULL,
    customer_id uuid NOT NULL,
    restaurant_id uuid NOT NULL,
    reservation_id uuid,
    group_booking_id uuid,
    corporate_booking_id uuid,
    takeaway_order_id uuid,
    dini_ticket_id uuid,
    order_type varchar(40) NOT NULL,
    status varchar(30) NOT NULL,
    subtotal numeric(18,2) NOT NULL,
    discount numeric(18,2) NOT NULL,
    tax numeric(18,2) NOT NULL,
    total numeric(18,2) NOT NULL,
    currency varchar(3) NOT NULL,
    special_instructions varchar(2000),
    UNIQUE(order_number), UNIQUE(id,restaurant_id), FOREIGN KEY(customer_id) REFERENCES customers(id), FOREIGN KEY(restaurant_id) REFERENCES restaurants(id), FOREIGN KEY(reservation_id,restaurant_id) REFERENCES reservations(id,restaurant_id), FOREIGN KEY(group_booking_id,restaurant_id) REFERENCES group_bookings(id,restaurant_id), FOREIGN KEY(corporate_booking_id,restaurant_id) REFERENCES corporate_bookings(id,restaurant_id), CHECK(subtotal>=0 AND discount>=0 AND tax>=0 AND total=subtotal-discount+tax AND total>=0), CHECK(order_type IN ('DINE_IN_PREORDER','DINE_IN','TAKEAWAY','ON_THE_WAY','DINITICKET_PREORDER')), CHECK(status IN ('PLACED','ACCEPTED','PREPARING','READY','SERVED','COMPLETED','CANCELLED','REJECTED'))
);
CREATE TABLE food_order_items (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    order_id uuid NOT NULL,
    menu_item_id uuid NOT NULL,
    variant_id uuid,
    item_name varchar(160) NOT NULL,
    variant_name varchar(80),
    quantity integer NOT NULL,
    base_unit_price numeric(18,2) NOT NULL,
    unit_price numeric(18,2) NOT NULL,
    subtotal numeric(18,2) NOT NULL,
    notes varchar(1000),
    FOREIGN KEY(order_id) REFERENCES food_orders(id), FOREIGN KEY(menu_item_id) REFERENCES menu_items(id), FOREIGN KEY(variant_id) REFERENCES menu_variants(id), CHECK(quantity BETWEEN 1 AND 99 AND unit_price>=0 AND base_unit_price>=unit_price AND subtotal=unit_price*quantity)
);
CREATE TABLE food_order_item_addons (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    order_item_id uuid NOT NULL,
    menu_addon_id uuid NOT NULL,
    name varchar(80) NOT NULL,
    price numeric(18,2) NOT NULL,
    FOREIGN KEY(order_item_id) REFERENCES food_order_items(id), FOREIGN KEY(menu_addon_id) REFERENCES menu_addons(id), UNIQUE(order_item_id,menu_addon_id), CHECK(price>=0)
);
CREATE TABLE takeaway_orders (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    order_number varchar(40) NOT NULL,
    food_order_id uuid NOT NULL,
    customer_id uuid NOT NULL,
    restaurant_id uuid NOT NULL,
    pickup_time timestamptz NOT NULL,
    status varchar(30) NOT NULL,
    subtotal numeric(18,2) NOT NULL,
    discount numeric(18,2) NOT NULL,
    tax numeric(18,2) NOT NULL,
    total numeric(18,2) NOT NULL,
    payment_status varchar(30) NOT NULL,
    pickup_code varchar(64) NOT NULL,
    collected_at timestamptz,
    UNIQUE(order_number), UNIQUE(food_order_id), UNIQUE(id,restaurant_id), FOREIGN KEY(food_order_id,restaurant_id) REFERENCES food_orders(id,restaurant_id), FOREIGN KEY(customer_id) REFERENCES customers(id), CHECK(status IN ('PLACED','ACCEPTED','PREPARING','READY_FOR_PICKUP','COLLECTED','CANCELLED','REJECTED')), CHECK(payment_status IN ('UNPAID','PAID','PARTIALLY_PAID','REFUNDED')), CHECK(subtotal>=0 AND discount>=0 AND tax>=0 AND total=subtotal-discount+tax AND total>=0)
);
CREATE TABLE on_the_way_order_tracking (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    takeaway_order_id uuid NOT NULL,
    consent boolean NOT NULL,
    consented_at timestamptz,
    customer_latitude numeric(10,7),
    customer_longitude numeric(10,7),
    restaurant_latitude numeric(10,7),
    restaurant_longitude numeric(10,7),
    estimated_arrival timestamptz,
    distance_km numeric(10,3),
    last_location_updated_at timestamptz,
    UNIQUE(takeaway_order_id), FOREIGN KEY(takeaway_order_id) REFERENCES takeaway_orders(id), CHECK(customer_latitude BETWEEN -90 AND 90 AND customer_longitude BETWEEN -180 AND 180), CHECK(consent OR (customer_latitude IS NULL AND customer_longitude IS NULL AND estimated_arrival IS NULL AND last_location_updated_at IS NULL))
);
CREATE TABLE order_status_history (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    order_id uuid NOT NULL,
    old_status varchar(30),
    new_status varchar(30) NOT NULL,
    actor_id uuid,
    reason varchar(1000),
    changed_at timestamptz NOT NULL,
    FOREIGN KEY(order_id) REFERENCES food_orders(id), FOREIGN KEY(actor_id) REFERENCES users(id)
);
ALTER TABLE food_orders ADD CONSTRAINT food_takeaway_restaurant_fk FOREIGN KEY(takeaway_order_id,restaurant_id) REFERENCES takeaway_orders(id,restaurant_id);
CREATE INDEX food_orders_customer_idx ON food_orders(customer_id,created_at DESC,id);
CREATE INDEX food_orders_owner_idx ON food_orders(restaurant_id,status,created_at,id);
CREATE INDEX food_orders_reservation_idx ON food_orders(reservation_id,status);
CREATE INDEX food_orders_group_idx ON food_orders(group_booking_id,status);
CREATE INDEX food_orders_corporate_idx ON food_orders(corporate_booking_id,status);
CREATE INDEX food_items_order_idx ON food_order_items(order_id,id);
CREATE INDEX takeaway_customer_idx ON takeaway_orders(customer_id,created_at DESC,id);
CREATE INDEX takeaway_owner_idx ON takeaway_orders(restaurant_id,status,pickup_time,id);
CREATE INDEX order_history_idx ON order_status_history(order_id,changed_at,id);
