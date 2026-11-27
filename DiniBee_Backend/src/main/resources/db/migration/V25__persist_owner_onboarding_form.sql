-- Keep the full owner-entered plans alongside the normalized onboarding configuration.
-- Existing missing values cannot be reconstructed by a schema migration.
ALTER TABLE restaurants ADD COLUMN onboarding_form jsonb;
ALTER TABLE restaurant_documents DROP CONSTRAINT restaurant_documents_document_type_check;
ALTER TABLE restaurant_documents ADD CONSTRAINT restaurant_documents_document_type_check
    CHECK (document_type IN ('FSSAI','GST','BUSINESS_REGISTRATION','OWNER_ID','PAN','LOGO','COVER_IMAGE','OTHER'));
