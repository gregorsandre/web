-- Exactly one conversation per pair: user_a is always the smaller uuid.
CREATE TABLE conversations (
                               id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               user_a          uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               user_b          uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               created_at      timestamptz NOT NULL DEFAULT now(),
                               last_message_at timestamptz NOT NULL DEFAULT now(),
                               CHECK (user_a < user_b),
                               UNIQUE (user_a, user_b)
);

CREATE TABLE messages (
                          id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          conversation_id bigint NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
                          sender_id       uuid   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          body            text   NOT NULL CHECK (length(body) BETWEEN 1 AND 2000),
                          created_at      timestamptz NOT NULL DEFAULT now(),
                          read_at         timestamptz                    -- NULL = unread by the recipient
);

