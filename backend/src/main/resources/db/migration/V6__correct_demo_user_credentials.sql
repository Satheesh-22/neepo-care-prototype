-- Local demonstration accounts only. Password: ChangeMe123!
UPDATE users
SET password_hash = '$2a$10$PzgK2zLqQdR0dtDStO/9luonlqUnMNb15ldkedHWlJpEq/UeWvWmK',
    enabled = TRUE
WHERE email IN ('dispatcher@neepo.local', 'driver@neepo.local');
