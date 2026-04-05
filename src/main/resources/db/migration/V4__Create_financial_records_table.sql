CREATE TABLE financial_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_by UUID NOT NULL REFERENCES users(id),
    category_id UUID NOT NULL REFERENCES categories(id),
    amount DECIMAL(15,2) NOT NULL,
    type VARCHAR(50) NOT NULL,
    transaction_date TIMESTAMPTZ NOT NULL,
    notes VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_financial_records_created_by ON financial_records(created_by);
CREATE INDEX idx_financial_records_category_id ON financial_records(category_id);
CREATE INDEX idx_financial_records_date ON financial_records(transaction_date);
CREATE INDEX idx_financial_records_deleted_at ON financial_records(deleted_at);
