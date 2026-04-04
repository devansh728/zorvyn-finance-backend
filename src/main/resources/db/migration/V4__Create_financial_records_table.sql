CREATE TABLE financial_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    category_id UUID NOT NULL REFERENCES categories(id),
    amount DECIMAL(15,2) NOT NULL,
    type VARCHAR(50) NOT NULL,
    description TEXT,
    transaction_date TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_financial_records_user_id ON financial_records(user_id);
CREATE INDEX idx_financial_records_category_id ON financial_records(category_id);
CREATE INDEX idx_financial_records_date ON financial_records(transaction_date);
