-- Production predates Flyway and has no CHECK constraint on smart_service.status; a fresh install gets
-- smart_service_status_check from V1. Drop whatever exists, then add one that knows DEACTIVATED and SUSPENDED.
DO $$
DECLARE
  status_check record;
BEGIN
  FOR status_check IN
    SELECT conname
    FROM pg_constraint
    WHERE conrelid = 'smart_service'::regclass
      AND contype = 'c'
      AND pg_get_constraintdef(oid) LIKE '%status%'
  LOOP
    EXECUTE format('ALTER TABLE smart_service DROP CONSTRAINT %I', status_check.conname);
  END LOOP;
END
$$;

ALTER TABLE smart_service
  ADD CONSTRAINT smart_service_status_check
  CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'DEACTIVATED', 'SUSPENDED'));

ALTER TABLE smart_service ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE smart_service ADD COLUMN deactivated_on TIMESTAMP(6) WITHOUT TIME ZONE;
ALTER TABLE smart_service ADD COLUMN deactivation_reason VARCHAR(1000);

CREATE TABLE deactivation_detection (
  detection_id VARCHAR(64) NOT NULL,
  client_id VARCHAR(255) NOT NULL,
  handled_on TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
  CONSTRAINT deactivation_detection_pkey PRIMARY KEY (detection_id)
);

CREATE TABLE domain_admin_user (
  sub VARCHAR(255) NOT NULL,
  email VARCHAR(255),
  last_login_on TIMESTAMP(6) WITHOUT TIME ZONE,
  CONSTRAINT domain_admin_user_pkey PRIMARY KEY (sub)
);
