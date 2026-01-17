CREATE TABLE expense_shares (
    id BIGSERIAL PRIMARY KEY,
    expense_id BIGINT NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount_owed DECIMAL(10,2) NOT NULL CHECK (amount_owed >= 0),
    UNIQUE(expense_id, user_id)
);

CREATE INDEX idx_expense_shares_expense_id ON expense_shares(expense_id);
CREATE INDEX idx_expense_shares_user_id ON expense_shares(user_id);

