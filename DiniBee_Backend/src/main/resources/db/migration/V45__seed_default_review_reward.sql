-- Ensure a successful visible customer review can earn the 10 points shown by the BookEat UI.
-- Do not create a duplicate when an administrator already configured a REVIEW rule.
INSERT INTO reward_rules (
    id, created_at, updated_at, version, rule_type, minimum_spend, points,
    conversion_value, active, effective_from, effective_until
)
SELECT gen_random_uuid(), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0,
       'REVIEW', 0, 10, 0, TRUE, CURRENT_TIMESTAMP, NULL
WHERE NOT EXISTS (SELECT 1 FROM reward_rules WHERE rule_type = 'REVIEW' AND active = TRUE);
