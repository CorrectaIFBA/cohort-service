-- A cohort is addressed in URLs as "{slug}-{code}". The code is short,
-- random and never changes, and it alone finds the cohort, so the slug is
-- only there to be read and two cohorts may share one.

ALTER TABLE cohorts ADD COLUMN IF NOT EXISTS public_code VARCHAR(6);

-- Six characters from [a-z0-9], drawn again on the rare clash.
DO $$
DECLARE
    cohort RECORD;
    candidate VARCHAR(6);
BEGIN
    FOR cohort IN SELECT id FROM cohorts WHERE public_code IS NULL LOOP
        LOOP
            SELECT string_agg(substr('abcdefghijklmnopqrstuvwxyz0123456789', 1 + floor(random() * 36)::int, 1), '')
            INTO candidate
            FROM generate_series(1, 6);
            EXIT WHEN NOT EXISTS (SELECT 1 FROM cohorts WHERE public_code = candidate);
        END LOOP;
        UPDATE cohorts SET public_code = candidate WHERE id = cohort.id;
    END LOOP;
END
$$;

ALTER TABLE cohorts ALTER COLUMN public_code SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_cohort_public_code') THEN
        ALTER TABLE cohorts ADD CONSTRAINT uk_cohort_public_code UNIQUE (public_code);
    END IF;
END
$$;

ALTER TABLE cohorts DROP CONSTRAINT IF EXISTS uk_cohort_slug;
