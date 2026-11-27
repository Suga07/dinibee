-- v28: re-sync the token sequence for databases that already applied V51
-- before legacy/imported DiniTicket rows were loaded.
SELECT setval(
  'dini_ticket_sequence',
  GREATEST(
    (SELECT last_value FROM dini_ticket_sequence),
    COALESCE((SELECT MAX(queue_sequence) FROM dini_tickets), 0),
    COALESCE((SELECT MAX(NULLIF(regexp_replace(ticket_number, '[^0-9]', '', 'g'), '')::bigint) FROM dini_tickets), 0),
    1
  ),
  true
);
