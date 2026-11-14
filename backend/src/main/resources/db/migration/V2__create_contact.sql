-- Create contacts for existing account schemas. On a new database, Hibernate's
-- schema update creates this table after it creates the account table.
DO $$
BEGIN
    IF to_regclass('public.account') IS NOT NULL THEN
        CREATE TABLE IF NOT EXISTS public.contact (
            account_id BIGINT NOT NULL REFERENCES public.account(id) ON DELETE CASCADE,
            contact_account_id BIGINT NOT NULL REFERENCES public.account(id) ON DELETE CASCADE,
            status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
            PRIMARY KEY (account_id, contact_account_id),
            CONSTRAINT contact_not_self CHECK (account_id <> contact_account_id)
        );
    END IF;
END $$;
