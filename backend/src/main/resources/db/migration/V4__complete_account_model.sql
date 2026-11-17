ALTER TABLE account ADD COLUMN IF NOT EXISTS name VARCHAR(100);
ALTER TABLE account ADD COLUMN IF NOT EXISTS two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE account ADD COLUMN IF NOT EXISTS sso_id VARCHAR(150);
ALTER TABLE account ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE account ADD COLUMN IF NOT EXISTS two_factor_secret VARCHAR(150);
ALTER TABLE account ADD COLUMN IF NOT EXISTS profile_picture_id BIGINT;

DO $$
BEGIN
    IF to_regclass('public.picture') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM pg_constraint
           WHERE conname = 'account_profile_picture_fk'
       ) THEN
        ALTER TABLE account
            ADD CONSTRAINT account_profile_picture_fk
            FOREIGN KEY (profile_picture_id) REFERENCES picture(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS account_sso_id_uq ON account (sso_id);
