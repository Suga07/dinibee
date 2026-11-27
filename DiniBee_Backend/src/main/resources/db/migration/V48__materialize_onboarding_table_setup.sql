-- Backfill real bookable tables for restaurants whose onboarding Table Setup saved
-- planned counts but no restaurant_tables inventory rows. Future onboarding writes are
-- materialized by OwnerOnboardingServiceImpl as well.
WITH missing AS (
    SELECT s.restaurant_id, s.planned_table_count, s.planned_seat_count,
           left(nullif(btrim(s.section_description), ''), 100) AS section
    FROM restaurant_table_setup s
    WHERE NOT EXISTS (
        SELECT 1 FROM restaurant_tables t
        WHERE t.restaurant_id=s.restaurant_id AND t.deleted_at IS NULL
    )
), generated AS (
    SELECT m.*, gs AS n,
           (m.planned_seat_count / m.planned_table_count)
           + CASE WHEN gs <= (m.planned_seat_count % m.planned_table_count) THEN 1 ELSE 0 END AS capacity
    FROM missing m
    CROSS JOIN LATERAL generate_series(1, m.planned_table_count) gs
)
INSERT INTO restaurant_tables
(id, restaurant_id, table_number, table_name, capacity, minimum_guests, maximum_guests,
 floor, section, shape, status, x_position, y_position, width, height, rotation, active)
SELECT gen_random_uuid(), restaurant_id, 'T'||n, 'Table '||n, capacity, 1, capacity,
       0, section, 'SQUARE', 'AVAILABLE', ((n-1)%4)*120, ((n-1)/4)*120,
       100, 100, 0, true
FROM generated;
