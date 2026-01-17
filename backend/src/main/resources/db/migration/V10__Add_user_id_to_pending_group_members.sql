ALTER TABLE pending_group_members
ADD COLUMN user_id BIGINT REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX idx_pending_group_members_user_id ON pending_group_members(user_id);
