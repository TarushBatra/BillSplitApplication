CREATE TABLE settlements (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    from_user_id BIGINT NOT NULL REFERENCES users(id),
    to_user_id BIGINT NOT NULL REFERENCES users(id),
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    message TEXT,
    image_url VARCHAR(500),
    settled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_settlements_group_id ON settlements(group_id);
CREATE INDEX idx_settlements_from_user_id ON settlements(from_user_id);
CREATE INDEX idx_settlements_to_user_id ON settlements(to_user_id);
CREATE INDEX idx_settlements_settled_at ON settlements(settled_at);
