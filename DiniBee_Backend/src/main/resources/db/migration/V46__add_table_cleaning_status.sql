-- Allow the owner app to persist the Cleaning operational state instead of keeping it in UI-only state.
ALTER TABLE restaurant_tables DROP CONSTRAINT IF EXISTS restaurant_tables_status_check;
ALTER TABLE restaurant_tables ADD CONSTRAINT restaurant_tables_status_check
    CHECK (status IN ('AVAILABLE','RESERVED','OCCUPIED','CLEANING','BLOCKED','MAINTENANCE'));
