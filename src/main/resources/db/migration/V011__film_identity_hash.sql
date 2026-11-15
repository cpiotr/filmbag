-- SHA-256 of title, year and genre names; replaces the 32-bit hash column for matching films.
-- Filled for existing films by the application at startup. The old hash column stays until
-- no running version writes it.
ALTER TABLE film ADD COLUMN identity_hash CHAR(64) NULL;
CREATE INDEX i_film_identity_hash ON film (identity_hash);
