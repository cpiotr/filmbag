ALTER TABLE film CHANGE COLUMN IF EXISTS score score DECIMAL(24, 18) NOT NULL;
ALTER TABLE score CHANGE COLUMN IF EXISTS grade grade DECIMAL(24, 18) NOT NULL;

CREATE INDEX IF NOT EXISTS i_film_year ON film (year);
CREATE INDEX IF NOT EXISTS i_film_score ON film (score);