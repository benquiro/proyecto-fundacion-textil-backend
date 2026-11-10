-- A leading zero adds nothing to the RUT check digit, so "01234567-4" and "1234567-4" are the
-- same person. The application strips leading zeros; the CHECK rejects them so the unique key
-- cannot hold two spellings of one RUT.
ALTER TABLE entrepreneur DROP CONSTRAINT ck_entrepreneur_rut_format;
ALTER TABLE entrepreneur
    ADD CONSTRAINT ck_entrepreneur_rut_format CHECK (rut ~ '^[1-9][0-9]{6,7}-[0-9K]$');
