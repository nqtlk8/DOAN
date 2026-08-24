-- ============================================================================
-- MODULE: IDENTITY (HQ ? master data)
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto; -- cho gen_random_uuid()

CREATE TABLE branch (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(20)  NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    address         VARCHAR(500),
    phone           VARCHAR(20),
    opening_hours   VARCHAR(100),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE user_account (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(100) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(200) NOT NULL,
    email           VARCHAR(200),
    phone           VARCHAR(20),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE role (
    id              SMALLSERIAL PRIMARY KEY,
    code            VARCHAR(50)  NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL
);

CREATE TABLE permission (
    id              SMALLSERIAL PRIMARY KEY,
    code            VARCHAR(100) NOT NULL UNIQUE,
    description     VARCHAR(255)
);

CREATE TABLE role_permission (
    role_id         SMALLINT NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    permission_id   SMALLINT NOT NULL REFERENCES permission(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_branch_role (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT   NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    role_id         SMALLINT NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    branch_id       BIGINT   REFERENCES branch(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX ux_user_branch_role ON user_branch_role (user_id, role_id, COALESCE(branch_id, 0));

