-- Preference model extension for profession, marital status, and looking-for defaults.
-- Apply before deploying the updated matching service to production databases.

ALTER TABLE preferences_men
  ADD COLUMN IF NOT EXISTS profession VARCHAR(80) NULL,
  ADD COLUMN IF NOT EXISTS marital_status VARCHAR(24) NULL,
  ADD COLUMN IF NOT EXISTS looking_for VARCHAR(256) NULL;

ALTER TABLE preferences_women
  ADD COLUMN IF NOT EXISTS profession VARCHAR(80) NULL,
  ADD COLUMN IF NOT EXISTS marital_status VARCHAR(24) NULL,
  ADD COLUMN IF NOT EXISTS looking_for VARCHAR(256) NULL;

ALTER TABLE preferences_gay
  ADD COLUMN IF NOT EXISTS profession VARCHAR(80) NULL,
  ADD COLUMN IF NOT EXISTS marital_status VARCHAR(24) NULL,
  ADD COLUMN IF NOT EXISTS looking_for VARCHAR(256) NULL;

ALTER TABLE preferences_lesbian
  ADD COLUMN IF NOT EXISTS profession VARCHAR(80) NULL,
  ADD COLUMN IF NOT EXISTS marital_status VARCHAR(24) NULL,
  ADD COLUMN IF NOT EXISTS looking_for VARCHAR(256) NULL;
