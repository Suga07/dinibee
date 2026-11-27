CREATE TABLE restaurant_cuisines (restaurant_id uuid NOT NULL REFERENCES restaurants(id), name varchar(80) NOT NULL CHECK (length(btrim(name))>0), PRIMARY KEY(restaurant_id,name));
CREATE INDEX restaurant_cuisines_lookup_idx ON restaurant_cuisines(lower(name),restaurant_id);
-- Populated only by the later verified-review workflow; no invented ratings.
CREATE TABLE restaurant_rating_summaries (restaurant_id uuid PRIMARY KEY REFERENCES restaurants(id), average_rating numeric(3,2) NOT NULL DEFAULT 0 CHECK(average_rating BETWEEN 0 AND 5), review_count bigint NOT NULL DEFAULT 0 CHECK(review_count>=0), CHECK(review_count>0 OR average_rating=0));
CREATE TABLE customer_favorite_restaurants (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0,
    customer_id uuid NOT NULL,
    restaurant_id uuid NOT NULL,
    FOREIGN KEY(customer_id) REFERENCES customers(id), FOREIGN KEY(restaurant_id) REFERENCES restaurants(id), UNIQUE(customer_id,restaurant_id)
);

CREATE TABLE request_idempotency (actor_id uuid NOT NULL REFERENCES users(id), scope varchar(60) NOT NULL, request_key varchar(100) NOT NULL, request_hash char(64) NOT NULL, resource_id uuid NOT NULL, created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(actor_id,scope,request_key));
CREATE INDEX restaurant_public_city_idx ON restaurants(lower(city),restaurant_name,id) WHERE approval_status='APPROVED' AND status='ACTIVE';
-- Resolve an ambiguous local opening to its earlier offset, matching java.time.
CREATE FUNCTION restaurant_local_open(local_stamp timestamp,zone_name text) RETURNS timestamptz LANGUAGE sql STABLE AS $$
WITH base AS (SELECT local_stamp AT TIME ZONE zone_name AS instant), candidate AS (SELECT instant, instant+((instant AT TIME ZONE zone_name)-((instant-interval '1 day') AT TIME ZONE zone_name)-interval '1 day') AS earlier FROM base)
SELECT CASE WHEN earlier AT TIME ZONE zone_name=local_stamp THEN least(instant,earlier) ELSE instant END FROM candidate;
$$;
-- Dated schedules override the complete date, including carry-in from yesterday.
CREATE FUNCTION restaurant_open_at(rid uuid, moment timestamptz) RETURNS boolean LANGUAGE sql STABLE AS $$
WITH local AS (SELECT (moment AT TIME ZONE timezone)::date AS day,timezone FROM restaurants WHERE id=rid AND timezone IS NOT NULL),
days AS (SELECT day,timezone,day+delta AS service_day FROM local CROSS JOIN generate_series(-1,0) AS g(delta)),
schedules AS (SELECT d.*, coalesce(s.closed,h.closed,true) AS closed,coalesce(s.opening_time,h.opening_time) AS opening_time,coalesce(s.closing_time,h.closing_time) AS closing_time,coalesce(s.closes_next_day,h.closes_next_day,false) AS next_day FROM days d LEFT JOIN restaurant_special_schedules s ON s.restaurant_id=rid AND s.date=d.service_day LEFT JOIN restaurant_operating_hours h ON h.restaurant_id=rid AND h.day_of_week=upper(trim(to_char(d.service_day,'DAY')))),
windows AS (SELECT restaurant_local_open(service_day+opening_time,timezone) AS starts_at, LEAST(((service_day+CASE WHEN next_day THEN 1 ELSE 0 END)+closing_time) AT TIME ZONE timezone,CASE WHEN next_day AND EXISTS(SELECT 1 FROM restaurant_special_schedules n WHERE n.restaurant_id=rid AND n.date=service_day+1) THEN (service_day+1)::timestamp AT TIME ZONE timezone ELSE 'infinity'::timestamptz END) AS ends_at FROM schedules WHERE NOT closed AND NOT(service_day<day AND EXISTS(SELECT 1 FROM restaurant_special_schedules c WHERE c.restaurant_id=rid AND c.date=day)))
SELECT EXISTS(SELECT 1 FROM windows WHERE starts_at<=moment AND moment<ends_at);
$$;
