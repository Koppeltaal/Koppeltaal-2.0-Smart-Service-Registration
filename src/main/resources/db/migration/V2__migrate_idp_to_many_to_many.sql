-- Migrate IDP columns from OneToOne to ManyToMany
-- V1 baseline contains the old schema with patient_idp, practitioner_idp, related_person_idp columns
-- This migration creates new join tables and migrates the data

-- Create join tables (if they don't exist yet from Hibernate)
CREATE TABLE IF NOT EXISTS smart_service_patient_idp (
    smart_service_id UUID NOT NULL,
    identity_provider_id UUID NOT NULL,
    PRIMARY KEY (smart_service_id, identity_provider_id),
    FOREIGN KEY (smart_service_id) REFERENCES smart_service(id) ON DELETE CASCADE,
    FOREIGN KEY (identity_provider_id) REFERENCES identity_provider(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS smart_service_practitioner_idp (
    smart_service_id UUID NOT NULL,
    identity_provider_id UUID NOT NULL,
    PRIMARY KEY (smart_service_id, identity_provider_id),
    FOREIGN KEY (smart_service_id) REFERENCES smart_service(id) ON DELETE CASCADE,
    FOREIGN KEY (identity_provider_id) REFERENCES identity_provider(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS smart_service_related_person_idp (
    smart_service_id UUID NOT NULL,
    identity_provider_id UUID NOT NULL,
    PRIMARY KEY (smart_service_id, identity_provider_id),
    FOREIGN KEY (smart_service_id) REFERENCES smart_service(id) ON DELETE CASCADE,
    FOREIGN KEY (identity_provider_id) REFERENCES identity_provider(id) ON DELETE CASCADE
);

-- Migrate data from old columns
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'smart_service' AND column_name = 'patient_idp'
    ) THEN
        -- Migrate patient_idp data
        INSERT INTO smart_service_patient_idp (smart_service_id, identity_provider_id)
        SELECT id, patient_idp FROM smart_service WHERE patient_idp IS NOT NULL;

        -- Migrate practitioner_idp data
        INSERT INTO smart_service_practitioner_idp (smart_service_id, identity_provider_id)
        SELECT id, practitioner_idp FROM smart_service WHERE practitioner_idp IS NOT NULL;

        -- Migrate related_person_idp data
        INSERT INTO smart_service_related_person_idp (smart_service_id, identity_provider_id)
        SELECT id, related_person_idp FROM smart_service WHERE related_person_idp IS NOT NULL;

        -- Drop old foreign key constraints
        ALTER TABLE smart_service DROP CONSTRAINT IF EXISTS patient_idp_fk;
        ALTER TABLE smart_service DROP CONSTRAINT IF EXISTS practitioner_idp_fk;
        ALTER TABLE smart_service DROP CONSTRAINT IF EXISTS related_person_idp_fk;

        -- Drop old columns
        ALTER TABLE smart_service DROP COLUMN patient_idp;
        ALTER TABLE smart_service DROP COLUMN practitioner_idp;
        ALTER TABLE smart_service DROP COLUMN related_person_idp;

        RAISE NOTICE 'IDP migration completed: data migrated from old columns to join tables';
    ELSE
        RAISE NOTICE 'IDP migration: join tables created for clean database';
    END IF;
END $$;
