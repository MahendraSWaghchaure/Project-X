-- Postgres refuses to create a partition for a range while the DEFAULT partition still holds rows that
-- belong to it, which used to make AttendancePartitionJob fail forever for that month (and every month after
-- it in the same call). The creator now moves any stranded rows of a missing month out of DEFAULT, creates the
-- partition and re-inserts them, all inside the calling session's transaction.
CREATE OR REPLACE FUNCTION ensure_school_attendance_partition(month_start DATE) RETURNS VOID AS $fn$
DECLARE
    first_day DATE := date_trunc('month', month_start)::date;
    next_first DATE := (date_trunc('month', month_start) + interval '1 month')::date;
    partition_name TEXT := 'school_attendance_y' || to_char(date_trunc('month', month_start), 'YYYY') || 'm'
                           || to_char(date_trunc('month', month_start), 'MM');
    stranded BIGINT;
BEGIN
    IF to_regclass(partition_name) IS NOT NULL THEN
        RETURN;
    END IF;
    CREATE TEMP TABLE IF NOT EXISTS _att_stranded (LIKE school_attendance_default) ON COMMIT DROP;
    TRUNCATE _att_stranded;
    WITH moved AS (
        DELETE FROM school_attendance_default
         WHERE attendance_date >= first_day AND attendance_date < next_first
        RETURNING *)
    INSERT INTO _att_stranded SELECT * FROM moved;
    GET DIAGNOSTICS stranded = ROW_COUNT;
    EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF school_attendance FOR VALUES FROM (%L) TO (%L)',
        partition_name, first_day, next_first);
    IF stranded > 0 THEN
        INSERT INTO school_attendance SELECT * FROM _att_stranded;
    END IF;
END;
$fn$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ensure_school_attendance_partitions(months_back INT, months_ahead INT) RETURNS VOID AS $fn$
DECLARE
    month_start DATE := date_trunc('month', now() - make_interval(months => months_back))::date;
    last_start DATE := date_trunc('month', now() + make_interval(months => months_ahead))::date;
BEGIN
    WHILE month_start <= last_start LOOP
        PERFORM ensure_school_attendance_partition(month_start);
        month_start := (month_start + interval '1 month')::date;
    END LOOP;
END;
$fn$ LANGUAGE plpgsql;
