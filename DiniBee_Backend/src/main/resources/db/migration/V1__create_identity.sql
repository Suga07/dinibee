CREATE TABLE users (

    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,

    first_name varchar(80) NOT NULL,
    last_name varchar(80) NOT NULL,
    email varchar(320) NOT NULL UNIQUE CHECK (email = lower(btrim(email))),
    password_hash varchar(100) NOT NULL,
    phone_number varchar(20),
    status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','BLOCKED','INACTIVE')),
    email_verified boolean NOT NULL DEFAULT false,
    phone_verified boolean NOT NULL DEFAULT false,
    token_version bigint NOT NULL DEFAULT 0 CHECK (token_version >= 0),
    firebase_uid varchar(128) UNIQUE,
    CHECK (NOT phone_verified OR phone_number IS NOT NULL)
);
-- Unverified contact details must not reserve a phone number indefinitely.
CREATE UNIQUE INDEX users_verified_phone_uq ON users(phone_number) WHERE phone_verified = true;
CREATE TABLE roles (

    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,

    code varchar(40) NOT NULL UNIQUE,
    description varchar(200) NOT NULL
);
CREATE TABLE permissions (

    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,

    code varchar(80) NOT NULL UNIQUE,
    description varchar(200) NOT NULL
);
CREATE TABLE user_roles (
    user_id uuid NOT NULL REFERENCES users(id),
    role_id uuid NOT NULL REFERENCES roles(id),
    PRIMARY KEY(user_id, role_id)
);
CREATE TABLE role_permissions (
    role_id uuid NOT NULL REFERENCES roles(id),
    permission_id uuid NOT NULL REFERENCES permissions(id),
    PRIMARY KEY(role_id, permission_id)
);
CREATE TABLE customers (

    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,

    user_id uuid NOT NULL UNIQUE REFERENCES users(id)
);
CREATE TABLE restaurant_owners (

    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,

    user_id uuid NOT NULL UNIQUE REFERENCES users(id)
);
CREATE INDEX user_roles_role_idx ON user_roles(role_id);
CREATE INDEX role_permissions_permission_idx ON role_permissions(permission_id);
