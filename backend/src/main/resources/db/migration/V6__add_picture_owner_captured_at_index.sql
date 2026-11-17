-- Support paginated picture lists ordered by capture time, then id, for one owner.
CREATE INDEX IF NOT EXISTS picture_owner_captured_at_idx
    ON picture (owner_id, captured_at DESC NULLS LAST, id);
