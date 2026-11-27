-- Ensure an active REVIEW reward rule exists in databases that had already
-- migrated past V45 before V45__seed_default_review_reward.sql was added.
UPDATE reward_rules
SET points = 10,
    updated_at = CURRENT_TIMESTAMP
WHERE rule_type = 'REVIEW'
  AND active = TRUE
  AND points = 0;

INSERT INTO reward_rules (
    id, created_at, updated_at, version, rule_type, minimum_spend, points,
    conversion_value, active, effective_from, effective_until
)
SELECT gen_random_uuid(), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0,
       'REVIEW', 0, 10, 0, TRUE, CURRENT_TIMESTAMP - INTERVAL '1 minute', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM reward_rules WHERE rule_type = 'REVIEW' AND active = TRUE
);
