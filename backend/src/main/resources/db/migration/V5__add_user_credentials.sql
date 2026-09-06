ALTER TABLE users ADD COLUMN password_hash VARCHAR(100);
ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;

-- Demo password: ChangeMe123! (local demonstration data only).
UPDATE users
SET password_hash = '$2a$10$wI8QzYlZpnvQ4MZYEi.IEOvlrntZmFE7FFwsI.Fp5sLo6yblhWPSK'
WHERE password_hash IS NULL;

ALTER TABLE users ALTER COLUMN password_hash SET NOT NULL;
