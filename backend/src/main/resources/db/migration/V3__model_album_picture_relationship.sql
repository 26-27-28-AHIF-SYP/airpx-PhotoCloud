CREATE SEQUENCE IF NOT EXISTS album_seq;

CREATE TABLE IF NOT EXISTS album (
    id BIGINT PRIMARY KEY DEFAULT nextval('album_seq'),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    owner_id BIGINT NOT NULL REFERENCES account(id),
    description VARCHAR(200),
    is_public_on_profile BOOLEAN NOT NULL DEFAULT FALSE,
    qr_code_token VARCHAR(256)
);

ALTER TABLE album ALTER COLUMN id TYPE BIGINT;
ALTER TABLE album ALTER COLUMN id SET DEFAULT nextval('album_seq');
ALTER TABLE album ALTER COLUMN name SET NOT NULL;
ALTER TABLE album ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE album ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE album ADD COLUMN IF NOT EXISTS description VARCHAR(200);
ALTER TABLE album ADD COLUMN IF NOT EXISTS is_public_on_profile BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE album ADD COLUMN IF NOT EXISTS qr_code_token VARCHAR(256);
ALTER SEQUENCE album_seq OWNED BY album.id;
SELECT setval('album_seq', COALESCE((SELECT MAX(id) FROM album), 0) + 1, FALSE);

CREATE TABLE IF NOT EXISTS album_pictures (
    album_id BIGINT NOT NULL REFERENCES album(id) ON DELETE CASCADE,
    picture_id BIGINT NOT NULL REFERENCES picture(id) ON DELETE CASCADE,
    PRIMARY KEY (album_id, picture_id)
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'picture' AND column_name = 'album_id'
    ) THEN
        INSERT INTO album_pictures (album_id, picture_id)
        SELECT album_id, id FROM picture WHERE album_id IS NOT NULL
        ON CONFLICT DO NOTHING;
        ALTER TABLE picture DROP COLUMN album_id;
    END IF;
END $$;
