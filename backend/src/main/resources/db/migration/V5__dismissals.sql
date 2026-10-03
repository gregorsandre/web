-- A dismissed user is never recommended again to the person who dismissed them.
CREATE TABLE dismissals (
                            user_id           uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            dismissed_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            created_at        timestamptz NOT NULL DEFAULT now(),
                            PRIMARY KEY (user_id, dismissed_user_id),
                            CHECK (user_id <> dismissed_user_id)
);