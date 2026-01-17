ALTER TABLE expenses 
ADD COLUMN deleted_at TIMESTAMP NULL,
ADD COLUMN deleted_by BIGINT NULL REFERENCES users(id);

CREATE INDEX idx_expenses_deleted_at ON expenses(deleted_at);
