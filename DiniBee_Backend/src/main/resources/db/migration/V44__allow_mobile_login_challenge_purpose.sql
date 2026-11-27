DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_attribute att ON att.attrelid = con.conrelid AND att.attnum = ANY(con.conkey)
        WHERE rel.relname = 'auth_challenges' AND att.attname = 'purpose' AND con.contype = 'c'
    ) LOOP
        EXECUTE 'ALTER TABLE auth_challenges DROP CONSTRAINT IF EXISTS ' || quote_ident(r.conname);
    END LOOP;
END $$;

ALTER TABLE auth_challenges ADD CONSTRAINT auth_challenges_purpose_check CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET', 'MOBILE_LOGIN'));
