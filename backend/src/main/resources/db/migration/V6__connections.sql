-- A pending row = outstanding request (requester -> addressee).
-- Accept sets status = 'accepted'. Decline or disconnect deletes the row.
CREATE TABLE connections (
                             id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             requester_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             addressee_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             status       text NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','accepted')),
                             created_at   timestamptz NOT NULL DEFAULT now(),
                             responded_at timestamptz,
                             CHECK (requester_id <> addressee_id)
);

-- One row per pair, regardless of who asked first (A->B and B->A count as the same pair)
CREATE UNIQUE INDEX connections_pair_unique
    ON connections (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id));

