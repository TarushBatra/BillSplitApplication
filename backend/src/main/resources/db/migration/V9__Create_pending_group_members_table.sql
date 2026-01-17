CREATE TABLE pending_group_members (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100),
    invited_by BIGINT NOT NULL REFERENCES users(id),
    invited_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(group_id, email)
);

CREATE INDEX idx_pending_group_members_group_id ON pending_group_members(group_id);
CREATE INDEX idx_pending_group_members_email ON pending_group_members(email);
