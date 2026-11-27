-- v39: keep public order IDs short and preserve ordered-food images.
ALTER TABLE food_order_items ADD COLUMN IF NOT EXISTS image_url varchar(2000);

UPDATE food_order_items foi
SET image_url = mi.image_url
FROM menu_items mi
WHERE foi.menu_item_id = mi.id
  AND (foi.image_url IS NULL OR btrim(foi.image_url) = '')
  AND mi.image_url IS NOT NULL;

-- Existing installations may contain old FO-.../UUID-style public numbers.
-- Convert only non-4-character rows. The internal UUID primary key is unchanged.
DO $$
DECLARE
  r record;
  candidate text;
  salt integer;
BEGIN
  FOR r IN
    SELECT id FROM food_orders
    WHERE order_number IS NULL OR order_number !~ '^[A-Z0-9]{4}$'
    ORDER BY created_at, id
  LOOP
    salt := 0;
    LOOP
      candidate := upper(substr(md5(r.id::text || ':' || salt::text), 1, 4));
      -- Avoid visually confusing O/I by remapping them.
      candidate := replace(replace(candidate, 'O', '8'), 'I', '7');
      EXIT WHEN NOT EXISTS (SELECT 1 FROM food_orders WHERE order_number = candidate AND id <> r.id);
      salt := salt + 1;
    END LOOP;
    UPDATE food_orders SET order_number = candidate WHERE id = r.id;
  END LOOP;
END $$;
