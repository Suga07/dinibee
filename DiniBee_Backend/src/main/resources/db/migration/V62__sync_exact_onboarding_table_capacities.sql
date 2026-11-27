-- Restore the exact per-table capacities captured in restaurants.onboarding_form.
-- Older onboarding materialisation only knew planned table/seat totals and averaged
-- capacities, which could turn 2x2 + 2x4 + 1x10 into incorrect 7/8-seat groups.

WITH desired AS (
    SELECT r.id AS restaurant_id,
           e.ordinality::int AS ord,
           btrim(e.item->>'tableNumber') AS table_number,
           greatest(1, least(30, (e.item->>'capacity')::int)) AS capacity,
           nullif(btrim(e.item->>'location'), '') AS section,
           coalesce((e.item->>'available')::boolean, true) AS active
    FROM restaurants r
    CROSS JOIN LATERAL jsonb_array_elements(coalesce(r.onboarding_form->'tables', '[]'::jsonb))
         WITH ORDINALITY AS e(item, ordinality)
    WHERE r.onboarding_form IS NOT NULL
      AND jsonb_typeof(r.onboarding_form->'tables') = 'array'
      AND nullif(btrim(e.item->>'tableNumber'), '') IS NOT NULL
      AND (e.item->>'capacity') ~ '^[0-9]+$'
)
UPDATE restaurant_tables t
SET capacity = d.capacity,
    minimum_guests = 1,
    maximum_guests = d.capacity,
    section = d.section,
    active = d.active
FROM desired d
WHERE t.restaurant_id = d.restaurant_id
  AND t.deleted_at IS NULL
  AND lower(btrim(t.table_number)) = lower(d.table_number);

WITH desired AS (
    SELECT r.id AS restaurant_id,
           e.ordinality::int AS ord,
           btrim(e.item->>'tableNumber') AS table_number,
           greatest(1, least(30, (e.item->>'capacity')::int)) AS capacity,
           nullif(btrim(e.item->>'location'), '') AS section,
           coalesce((e.item->>'available')::boolean, true) AS active
    FROM restaurants r
    CROSS JOIN LATERAL jsonb_array_elements(coalesce(r.onboarding_form->'tables', '[]'::jsonb))
         WITH ORDINALITY AS e(item, ordinality)
    WHERE r.onboarding_form IS NOT NULL
      AND jsonb_typeof(r.onboarding_form->'tables') = 'array'
      AND nullif(btrim(e.item->>'tableNumber'), '') IS NOT NULL
      AND (e.item->>'capacity') ~ '^[0-9]+$'
)
INSERT INTO restaurant_tables
(id, restaurant_id, table_number, table_name, capacity, minimum_guests, maximum_guests,
 floor, section, shape, status, x_position, y_position, width, height, rotation, active)
SELECT gen_random_uuid(), d.restaurant_id, d.table_number, d.table_number, d.capacity, 1, d.capacity,
       0, d.section, 'RECTANGLE', 'AVAILABLE', ((d.ord-1)%5)*140, ((d.ord-1)/5)*110,
       120, 80, 0, d.active
FROM desired d
WHERE NOT EXISTS (
    SELECT 1 FROM restaurant_tables t
    WHERE t.restaurant_id=d.restaurant_id
      AND t.deleted_at IS NULL
      AND lower(btrim(t.table_number))=lower(d.table_number)
);

-- Disable only leftover rows produced by the old automatic T1/Table 1 materialiser.
-- Do not touch rows with a current/future live reservation.
WITH form_restaurants AS (
    SELECT r.id AS restaurant_id
    FROM restaurants r
    WHERE r.onboarding_form IS NOT NULL
      AND jsonb_typeof(r.onboarding_form->'tables')='array'
      AND jsonb_array_length(r.onboarding_form->'tables') > 0
), desired_numbers AS (
    SELECT r.id AS restaurant_id, lower(btrim(e.item->>'tableNumber')) AS table_number
    FROM restaurants r
    CROSS JOIN LATERAL jsonb_array_elements(coalesce(r.onboarding_form->'tables', '[]'::jsonb)) e(item)
    WHERE r.onboarding_form IS NOT NULL
      AND jsonb_typeof(r.onboarding_form->'tables')='array'
      AND nullif(btrim(e.item->>'tableNumber'), '') IS NOT NULL
)
UPDATE restaurant_tables t
SET active=false
FROM form_restaurants fr
WHERE t.restaurant_id=fr.restaurant_id
  AND t.deleted_at IS NULL
  AND t.active=true
  AND t.table_number ~* '^T[0-9]+$'
  AND coalesce(t.table_name,'') ~* '^Table[[:space:]]+[0-9]+$'
  AND NOT EXISTS (
      SELECT 1 FROM desired_numbers d
      WHERE d.restaurant_id=t.restaurant_id
        AND d.table_number=lower(btrim(t.table_number))
  )
  AND NOT EXISTS (
      SELECT 1 FROM reservations r
      WHERE r.restaurant_id=t.restaurant_id
        AND r.table_id=t.id
        AND r.reservation_date >= current_date
        AND r.status IN ('PENDING','CONFIRMED','ARRIVED','CHECKED_IN','DINING')
  );
