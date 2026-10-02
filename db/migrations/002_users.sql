-- Up Migration
CREATE TABLE users (
                       id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                       email         citext NOT NULL UNIQUE,      -- case-insensitive, private: never returned by the API
                       password_hash text   NOT NULL,             -- bcrypt hash (salt is embedded in the hash)
                       created_at    timestamptz NOT NULL DEFAULT now()
);

-- Down Migration
DROP TABLE users;