UPDATE film set link = '' where link is null;
UPDATE film set title = '' where title is null;

ALTER TABLE film CHANGE COLUMN link link varchar(512) NOT NULL;
ALTER TABLE film CHANGE COLUMN poster poster varchar(512);
ALTER TABLE film CHANGE COLUMN title title varchar(255) NOT NULL;
ALTER TABLE film CHANGE COLUMN hash hash bigint(20) NOT NULL DEFAULT -1;
