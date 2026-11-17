-- Preserve existing uploaded-picture metadata while aligning the table name.
DO $$
BEGIN
    IF to_regclass('public.photo') IS NOT NULL AND to_regclass('public.picture') IS NULL THEN
        ALTER TABLE public.photo RENAME TO picture;
    ELSIF to_regclass('public.photo') IS NOT NULL AND to_regclass('public.picture') IS NOT NULL THEN
        RAISE EXCEPTION 'Both public.photo and public.picture exist; merge them before applying this migration';
    END IF;
END $$;
