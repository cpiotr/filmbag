ALTER TABLE score ADD COLUMN type VARCHAR(32) NULL;

UPDATE score set type = "CRITIC" where quantity < 100;
UPDATE score set type = "AMATEUR" where quantity >= 100;
