-- Phase 3: additive restaurant onboarding schema. V1-V3 remain unchanged.
CREATE TABLE restaurants (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    owner_id uuid NOT NULL REFERENCES restaurant_owners(id),
    restaurant_name varchar(160) NOT NULL,
    description varchar(3000),
    restaurant_type varchar(40) NOT NULL CHECK (restaurant_type IN ('RESTAURANT','CAFE','QUICK_SERVICE','FINE_DINING','CLOUD_KITCHEN','FOOD_COURT')),
    email varchar(320) NOT NULL,
    phone varchar(20) NOT NULL,
    alternate_phone varchar(20),
    address_line1 varchar(250),
    address_line2 varchar(250),
    city varchar(100),
    state varchar(100),
    postal_code varchar(20),
    country varchar(2),
    latitude numeric(10,7),
    longitude numeric(10,7),
    timezone varchar(60),
    business_registration_number varchar(100),
    gst_number varchar(30),
    fssai_number varchar(30),
    logo_url varchar(2048),
    cover_image_url varchar(2048),
    approval_status varchar(40) NOT NULL CHECK (approval_status IN ('DRAFT','SUBMITTED','UNDER_REVIEW','CHANGES_REQUESTED','APPROVED','REJECTED','SUSPENDED')),
    status varchar(40) NOT NULL CHECK (status IN ('INACTIVE','ACTIVE','TEMPORARILY_CLOSED','SUSPENDED')),
    submitted_at timestamptz,
    approved_at timestamptz,
    approved_by uuid REFERENCES users(id),
    rejection_reason varchar(2000),
    change_request_reason varchar(2000),
    change_sequence bigint NOT NULL,
    CHECK (latitude BETWEEN -90 AND 90), CHECK (longitude BETWEEN -180 AND 180),
    CHECK (approval_status <> 'APPROVED' OR (approved_at IS NOT NULL AND approved_by IS NOT NULL)),
    CHECK (status <> 'ACTIVE' OR approval_status = 'APPROVED')
);

CREATE TABLE restaurant_service_activations (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL REFERENCES restaurants(id),
    service_type varchar(40) NOT NULL CHECK (service_type IN ('RESTAURANT','TAKEAWAY','DINITICKET')),
    requested boolean NOT NULL,
    authorized boolean NOT NULL,
    enabled boolean NOT NULL,
    activated_at timestamptz,
    disabled_at timestamptz,
    UNIQUE (restaurant_id,service_type),
    CHECK (NOT enabled OR (requested AND authorized))
);

CREATE TABLE restaurant_operating_hours (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL REFERENCES restaurants(id),
    day_of_week varchar(40) NOT NULL CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
    opening_time time,
    closing_time time,
    closed boolean NOT NULL,
    closes_next_day boolean NOT NULL,
    UNIQUE (restaurant_id,day_of_week),
    CHECK ((closed AND opening_time IS NULL AND closing_time IS NULL AND NOT closes_next_day) OR (NOT closed AND opening_time IS NOT NULL AND closing_time IS NOT NULL AND ((NOT closes_next_day AND opening_time < closing_time) OR (closes_next_day AND closing_time <= opening_time))))
);

CREATE TABLE restaurant_special_schedules (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL REFERENCES restaurants(id),
    date date NOT NULL,
    closed boolean NOT NULL,
    opening_time time,
    closing_time time,
    closes_next_day boolean NOT NULL,
    reason varchar(500) NOT NULL,
    UNIQUE (restaurant_id,date),
    CHECK ((closed AND opening_time IS NULL AND closing_time IS NULL AND NOT closes_next_day) OR (NOT closed AND opening_time IS NOT NULL AND closing_time IS NOT NULL AND ((NOT closes_next_day AND opening_time < closing_time) OR (closes_next_day AND closing_time <= opening_time))))
);

CREATE TABLE restaurant_documents (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL REFERENCES restaurants(id),
    document_type varchar(40) NOT NULL CHECK (document_type IN ('FSSAI','GST','BUSINESS_REGISTRATION','OWNER_ID','PAN','OTHER')),
    document_number varchar(100) NOT NULL,
    file_url varchar(2048) NOT NULL,
    file_key varchar(250) NOT NULL,
    storage_provider varchar(40) NOT NULL CHECK (storage_provider IN ('LOCAL','S3')),
    original_filename varchar(150) NOT NULL,
    content_type varchar(100) NOT NULL,
    size_bytes bigint NOT NULL,
    sha256 varchar(64) NOT NULL,
    verification_status varchar(40) NOT NULL CHECK (verification_status IN ('PENDING','VERIFIED','REJECTED')),
    verified_by uuid REFERENCES users(id),
    verified_at timestamptz,
    rejection_reason varchar(2000),
    deleted_at timestamptz,
    CHECK (size_bytes BETWEEN 1 AND 5242880),
    CHECK (verification_status <> 'VERIFIED' OR (verified_at IS NOT NULL AND verified_by IS NOT NULL))
);

CREATE TABLE restaurant_reservation_settings (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL UNIQUE REFERENCES restaurants(id),
    reservation_enabled boolean NOT NULL,
    minimum_guests integer NOT NULL,
    maximum_guests_per_booking integer NOT NULL,
    slot_duration_minutes integer NOT NULL,
    reservation_duration_minutes integer NOT NULL,
    advance_booking_days integer NOT NULL,
    minimum_advance_minutes integer NOT NULL,
    grace_period_minutes integer NOT NULL,
    cancellation_allowed boolean NOT NULL,
    cancellation_before_minutes integer NOT NULL,
    auto_confirm boolean NOT NULL,
    special_request_allowed boolean NOT NULL,
    pre_order_allowed boolean NOT NULL,
    gps_check_in_enabled boolean NOT NULL,
    qr_check_in_enabled boolean NOT NULL,
    gps_check_in_radius_meters integer NOT NULL,
    CHECK (minimum_guests > 0 AND maximum_guests_per_booking >= minimum_guests),
    CHECK (slot_duration_minutes > 0 AND reservation_duration_minutes >= slot_duration_minutes),
    CHECK (advance_booking_days BETWEEN 1 AND 365 AND minimum_advance_minutes >= 0),
    CHECK (grace_period_minutes >= 0 AND cancellation_before_minutes >= 0),
    CHECK (gps_check_in_radius_meters BETWEEN 10 AND 1000)
);

CREATE TABLE restaurant_takeaway_settings (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL UNIQUE REFERENCES restaurants(id),
    enabled boolean NOT NULL,
    regular_takeaway_enabled boolean NOT NULL,
    on_the_way_enabled boolean NOT NULL,
    minimum_order_amount numeric(12,2) NOT NULL,
    average_preparation_minutes integer NOT NULL,
    maximum_advance_order_minutes integer NOT NULL,
    accept_cash boolean NOT NULL,
    accept_online_payment boolean NOT NULL,
    CHECK (minimum_order_amount >= 0 AND average_preparation_minutes > 0 AND maximum_advance_order_minutes >= average_preparation_minutes),
    CHECK (NOT enabled OR ((regular_takeaway_enabled OR on_the_way_enabled) AND (accept_cash OR accept_online_payment)))
);

CREATE TABLE diniticket_settings (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL UNIQUE REFERENCES restaurants(id),
    enabled boolean NOT NULL,
    maximum_queue_size integer NOT NULL,
    default_estimated_minutes_per_party integer NOT NULL,
    allow_pre_order boolean NOT NULL,
    allow_remote_join boolean NOT NULL,
    ticket_expiry_minutes integer NOT NULL,
    auto_expire_tickets boolean NOT NULL,
    CHECK (maximum_queue_size > 0 AND default_estimated_minutes_per_party > 0 AND ticket_expiry_minutes > 0)
);

CREATE TABLE restaurant_policies (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL UNIQUE REFERENCES restaurants(id),
    cancellation_policy varchar(3000) NOT NULL,
    refund_policy varchar(3000) NOT NULL,
    child_policy varchar(3000),
    pet_policy varchar(3000),
    dress_code varchar(3000),
    additional_policy varchar(3000)
);

CREATE TABLE restaurant_settlement_profiles (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL UNIQUE REFERENCES restaurants(id),
    account_holder_name varchar(160) NOT NULL,
    bank_name varchar(160) NOT NULL,
    encrypted_account_number varchar(500) NOT NULL,
    account_number_last4 varchar(4) NOT NULL,
    ifsc_code varchar(11) NOT NULL,
    upi_id varchar(150),
    tax_name varchar(160) NOT NULL,
    gst_number varchar(30),
    verified boolean NOT NULL,
    verified_by uuid REFERENCES users(id),
    verified_at timestamptz,
    rejection_reason varchar(2000),
    CHECK (NOT verified OR (verified_at IS NOT NULL AND verified_by IS NOT NULL))
);

CREATE TABLE restaurant_table_setup (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL UNIQUE REFERENCES restaurants(id),
    planned_table_count integer NOT NULL,
    planned_seat_count integer NOT NULL,
    section_description varchar(500),
    CHECK (planned_table_count > 0 AND planned_seat_count >= planned_table_count AND planned_seat_count <= planned_table_count * 30)
);

CREATE TABLE restaurant_menu_setup (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL UNIQUE REFERENCES restaurants(id),
    mode varchar(40) NOT NULL CHECK (mode IN ('CREATE_IN_MENU_MODULE','EXTERNAL_MENU')),
    external_menu_url varchar(2048),
    notes varchar(1000),
    acknowledged boolean NOT NULL
);

CREATE TABLE restaurant_team_access (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL UNIQUE REFERENCES restaurants(id),
    mode varchar(40) NOT NULL CHECK (mode IN ('OWNER_ONLY','DRAFT_TEAM')),
    acknowledged boolean NOT NULL
);

CREATE TABLE restaurant_team_drafts (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL REFERENCES restaurants(id),
    email varchar(320) NOT NULL,
    role varchar(40) NOT NULL CHECK (role IN ('RESTAURANT_MANAGER','RESTAURANT_STAFF')),
    UNIQUE (restaurant_id,email)
);

CREATE TABLE restaurant_onboarding_progress (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL UNIQUE REFERENCES restaurants(id),
    current_step varchar(40) NOT NULL CHECK (current_step IN ('RESTAURANT_DETAILS','LOCATION','BUSINESS_INFO','HOURS','SERVICES','TABLE_SETUP','RESERVATION_SETTINGS','TAKEAWAY_SETTINGS','DINITICKET_SETTINGS','MENU_SETUP','PAYMENT_DETAILS','POLICIES','TEAM_ACCESS','REVIEW')),
    restaurant_details_complete boolean NOT NULL,
    location_complete boolean NOT NULL,
    business_info_complete boolean NOT NULL,
    hours_complete boolean NOT NULL,
    services_complete boolean NOT NULL,
    table_setup_complete boolean NOT NULL,
    reservation_settings_complete boolean NOT NULL,
    takeaway_settings_complete boolean NOT NULL,
    diniticket_settings_complete boolean NOT NULL,
    menu_setup_complete boolean NOT NULL,
    payment_details_complete boolean NOT NULL,
    policies_complete boolean NOT NULL,
    team_access_complete boolean NOT NULL,
    submitted boolean NOT NULL
);

CREATE TABLE restaurant_approval_history (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    restaurant_id uuid NOT NULL REFERENCES restaurants(id),
    actor_id uuid NOT NULL REFERENCES users(id),
    reference_id uuid,
    action varchar(40) NOT NULL CHECK (action IN ('SUBMIT','UNDER_REVIEW','APPROVE','REJECT','REQUEST_CHANGES','SUSPEND','REACTIVATE','SERVICE_CHANGE','DOCUMENT_VERIFY','SETTLEMENT_VERIFY')),
    from_status varchar(40) NOT NULL CHECK (from_status IN ('DRAFT','SUBMITTED','UNDER_REVIEW','CHANGES_REQUESTED','APPROVED','REJECTED','SUSPENDED')),
    to_status varchar(40) NOT NULL CHECK (to_status IN ('DRAFT','SUBMITTED','UNDER_REVIEW','CHANGES_REQUESTED','APPROVED','REJECTED','SUSPENDED')),
    reason varchar(2000),
    service_type varchar(40) CHECK (service_type IN ('RESTAURANT','TAKEAWAY','DINITICKET'))
);

CREATE INDEX restaurants_owner_idx ON restaurants(owner_id,created_at,id);

CREATE INDEX restaurants_review_idx ON restaurants(approval_status,submitted_at,id);

CREATE INDEX restaurants_city_idx ON restaurants(lower(city));

CREATE UNIQUE INDEX restaurant_documents_current_type_uq ON restaurant_documents(restaurant_id,document_type) WHERE deleted_at IS NULL AND document_type <> 'OTHER';

CREATE INDEX restaurant_documents_lookup_idx ON restaurant_documents(restaurant_id,deleted_at);

CREATE INDEX restaurant_approval_history_lookup_idx ON restaurant_approval_history(restaurant_id,created_at,id);
