-- Add order column to IDP join tables for explicit ordering

-- Add order column to patient IDP join table
ALTER TABLE smart_service_patient_idp
ADD COLUMN idp_order INTEGER NOT NULL DEFAULT 0;

-- Add order column to practitioner IDP join table
ALTER TABLE smart_service_practitioner_idp
ADD COLUMN idp_order INTEGER NOT NULL DEFAULT 0;

-- Add order column to related person IDP join table
ALTER TABLE smart_service_related_person_idp
ADD COLUMN idp_order INTEGER NOT NULL DEFAULT 0;

-- Set initial order based on current data (ordered by identity_provider_id)
-- Patient IDPs
WITH ordered_patient_idps AS (
    SELECT
        smart_service_id,
        identity_provider_id,
        ROW_NUMBER() OVER (PARTITION BY smart_service_id ORDER BY identity_provider_id) - 1 as new_order
    FROM smart_service_patient_idp
)
UPDATE smart_service_patient_idp ssp
SET idp_order = opi.new_order
FROM ordered_patient_idps opi
WHERE ssp.smart_service_id = opi.smart_service_id
  AND ssp.identity_provider_id = opi.identity_provider_id;

-- Practitioner IDPs
WITH ordered_practitioner_idps AS (
    SELECT
        smart_service_id,
        identity_provider_id,
        ROW_NUMBER() OVER (PARTITION BY smart_service_id ORDER BY identity_provider_id) - 1 as new_order
    FROM smart_service_practitioner_idp
)
UPDATE smart_service_practitioner_idp ssp
SET idp_order = opi.new_order
FROM ordered_practitioner_idps opi
WHERE ssp.smart_service_id = opi.smart_service_id
  AND ssp.identity_provider_id = opi.identity_provider_id;

-- Related Person IDPs
WITH ordered_related_person_idps AS (
    SELECT
        smart_service_id,
        identity_provider_id,
        ROW_NUMBER() OVER (PARTITION BY smart_service_id ORDER BY identity_provider_id) - 1 as new_order
    FROM smart_service_related_person_idp
)
UPDATE smart_service_related_person_idp ssp
SET idp_order = opi.new_order
FROM ordered_related_person_idps opi
WHERE ssp.smart_service_id = opi.smart_service_id
  AND ssp.identity_provider_id = opi.identity_provider_id;
