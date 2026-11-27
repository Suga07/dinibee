-- v27: keep DiniTicket numbers monotonic after restores/imports/manual seed data.
-- PostgreSQL sequences are not advanced when rows are inserted with explicit values.
SELECT setval(
  'dini_ticket_sequence',
  GREATEST(
    COALESCE((SELECT MAX(queue_sequence) FROM dini_tickets), 0),
    COALESCE((SELECT MAX(NULLIF(regexp_replace(ticket_number, '[^0-9]', '', 'g'), '')::bigint) FROM dini_tickets), 0),
    1
  ),
  true
);
