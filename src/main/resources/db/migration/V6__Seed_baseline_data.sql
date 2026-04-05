INSERT INTO users (id, email, password_hash, full_name, role, status, failed_login_attempts, version)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@zorvyn.com',
    '$2a$12$Nq/D.i6yS81C8K7Qx46jzeJ5wFm5FzBwX6G436xRYf1YfCgJp0I1O',
    'System Admin',
    'ADMIN',
    'ACTIVE',
    0,
    0
);
INSERT INTO categories (name, type, is_system, created_by)
VALUES
    ('Salary', 'INCOME', TRUE, '00000000-0000-0000-0000-000000000001'),
    ('Rent', 'EXPENSE', TRUE, '00000000-0000-0000-0000-000000000001'),
    ('Groceries', 'EXPENSE', TRUE, '00000000-0000-0000-0000-000000000001'),
    ('Utilities', 'EXPENSE', TRUE, '00000000-0000-0000-0000-000000000001'),
    ('Entertainment', 'EXPENSE', TRUE, '00000000-0000-0000-0000-000000000001'),
    ('Healthcare', 'EXPENSE', TRUE, '00000000-0000-0000-0000-000000000001');
