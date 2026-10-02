-- Up Migration
-- Radius search (ST_DWithin) on the generated location column
CREATE INDEX bios_location_gist ON bios USING GIST (location);

-- Shared-sport lookups
CREATE INDEX user_tags_tag_id_idx ON user_tags (tag_id);

-- Connection lookups from either side
CREATE INDEX connections_requester_idx ON connections (requester_id, status);
CREATE INDEX connections_addressee_idx ON connections (addressee_id, status);

-- Chat list ordered by most recent
CREATE INDEX conversations_user_a_idx ON conversations (user_a, last_message_at DESC);
CREATE INDEX conversations_user_b_idx ON conversations (user_b, last_message_at DESC);

-- Paginated history (newest first) and unread counts
CREATE INDEX messages_conversation_idx ON messages (conversation_id, id DESC);
CREATE INDEX messages_unread_idx ON messages (conversation_id, sender_id) WHERE read_at IS NULL;

-- Down Migration
DROP INDEX messages_unread_idx;
DROP INDEX messages_conversation_idx;
DROP INDEX conversations_user_b_idx;
DROP INDEX conversations_user_a_idx;
DROP INDEX connections_addressee_idx;
DROP INDEX connections_requester_idx;
DROP INDEX user_tags_tag_id_idx;
DROP INDEX bios_location_gist;