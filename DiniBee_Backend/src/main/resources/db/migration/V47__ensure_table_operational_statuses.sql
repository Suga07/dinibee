-- Forward-only migration: extend restaurant table operational statuses.
ALTER TABLE restaurant_tables DROP CONSTRAINT IF EXISTS restaurant_tables_status_check;
ALTER TABLE restaurant_tables ADD CONSTRAINT restaurant_tables_status_check
    CHECK (status IN ('AVAILABLE','RESERVED','OCCUPIED','CLEANING','BLOCKED','MAINTENANCE'));
