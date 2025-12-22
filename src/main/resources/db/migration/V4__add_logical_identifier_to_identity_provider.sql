-- Add logical_identifier column to identity_provider table
ALTER TABLE identity_provider ADD COLUMN logical_identifier VARCHAR(255);

-- Add unique index on logical_identifier (provides both uniqueness constraint and fast lookups)
CREATE UNIQUE INDEX idx_identity_provider_logical_identifier ON identity_provider (logical_identifier);
