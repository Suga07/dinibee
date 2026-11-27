CREATE TABLE reviews (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    customer_id uuid NOT NULL,
    restaurant_id uuid NOT NULL,
    reservation_id uuid,
    food_order_id uuid,
    rating integer NOT NULL,
    food_rating integer,
    service_rating integer,
    ambience_rating integer,
    comment varchar(3000),
    owner_reply varchar(2000),
    status varchar(20) NOT NULL,
    moderation_reason varchar(1000),
    FOREIGN KEY(customer_id) REFERENCES customers(id), FOREIGN KEY(restaurant_id) REFERENCES restaurants(id), FOREIGN KEY(reservation_id) REFERENCES reservations(id), FOREIGN KEY(food_order_id) REFERENCES food_orders(id), CHECK((reservation_id IS NULL)<>(food_order_id IS NULL)), CHECK(rating BETWEEN 1 AND 5), CHECK(food_rating BETWEEN 1 AND 5), CHECK(service_rating BETWEEN 1 AND 5), CHECK(ambience_rating BETWEEN 1 AND 5), UNIQUE(customer_id,reservation_id), UNIQUE(customer_id,food_order_id)
);
CREATE TABLE support_tickets (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    ticket_number varchar(40) NOT NULL,
    created_by_user uuid NOT NULL,
    creator_type varchar(20) NOT NULL,
    category varchar(80) NOT NULL,
    subject varchar(160) NOT NULL,
    description varchar(5000) NOT NULL,
    priority varchar(20) NOT NULL,
    status varchar(30) NOT NULL,
    assigned_admin uuid,
    resolved_at timestamptz,
    FOREIGN KEY(created_by_user) REFERENCES users(id), FOREIGN KEY(assigned_admin) REFERENCES users(id), UNIQUE(ticket_number)
);
CREATE TABLE support_messages (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    ticket_id uuid NOT NULL,
    sender_user uuid NOT NULL,
    message varchar(5000) NOT NULL,
    attachment_url varchar(1000),
    FOREIGN KEY(ticket_id) REFERENCES support_tickets(id), FOREIGN KEY(sender_user) REFERENCES users(id)
);
CREATE TABLE notifications (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    user_id uuid NOT NULL,
    title varchar(160) NOT NULL,
    message varchar(2000) NOT NULL,
    notification_type varchar(60) NOT NULL,
    reference_type varchar(40) NOT NULL,
    reference_id uuid NOT NULL,
    read_flag boolean NOT NULL,
    deduplication_key varchar(200) NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id), UNIQUE(user_id,deduplication_key)
);
CREATE INDEX review_public_idx ON reviews(restaurant_id,status,created_at DESC,id);
CREATE INDEX support_creator_idx ON support_tickets(created_by_user,created_at DESC,id);
CREATE INDEX support_messages_idx ON support_messages(ticket_id,created_at,id);
CREATE INDEX notification_user_idx ON notifications(user_id,created_at DESC,id);
