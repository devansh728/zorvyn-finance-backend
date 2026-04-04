-- Insert default Admin user (Password is 'Admin@12345' hashed with BCrypt strength 12)
INSERT INTO users (id, email, password_hash, first_name, last_name, role, status)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@zorvyn.com',
    '$2a$12$Nq/D.i6yS81C8K7Qx46jzeJ5wFm5FzBwX6G436xRYf1YfCgJp0I1O',
    'System',
    'Admin',
    'ADMIN',
    'ACTIVE'
);

-- Insert System Categories
INSERT INTO categories (name, description, system_default, created_by, version)
VALUES
    ('Salary', 'Monthly salary income', TRUE, '00000000-0000-0000-0000-000000000001', 0),
    ('Rent', 'Housing rent expenses', TRUE, '00000000-0000-0000-0000-000000000001', 0),
    ('Groceries', 'Food and household supplies', TRUE, '00000000-0000-0000-0000-000000000001', 0),
    ('Utilities', 'Electricity, water, internet', TRUE, '00000000-0000-0000-0000-000000000001', 0),
    ('Entertainment', 'Movies, dining out, hobbies', TRUE, '00000000-0000-0000-0000-000000000001', 0),
    ('Healthcare', 'Medical expenses, pharmacy', TRUE, '00000000-0000-0000-0000-000000000001', 0);
