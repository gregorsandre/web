-- One profile picture per user. The random id is also the picture's public link
-- (/avatars/{id}), and profiles.avatar_path points at it.
CREATE TABLE avatars (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      uuid NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    content_type text NOT NULL CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    data         bytea NOT NULL,
    created_at   timestamptz NOT NULL DEFAULT now()
);
