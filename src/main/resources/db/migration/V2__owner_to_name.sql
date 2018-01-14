ALTER TABLE feat RENAME COLUMN character_owner TO character_name;

ALTER TABLE feat ADD UNIQUE (character_name, name);