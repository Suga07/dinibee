-- restaurant_service_activations
ALTER TABLE restaurant_service_activations DROP CONSTRAINT restaurant_service_activations_service_type_check;
UPDATE restaurant_service_activations SET service_type = 'DINE_IN' WHERE service_type = 'RESTAURANT';
ALTER TABLE restaurant_service_activations ADD CONSTRAINT restaurant_service_activations_service_type_check CHECK (service_type IN ('DINE_IN','TAKEAWAY','DINITICKET'));

-- restaurant_approval_history
ALTER TABLE restaurant_approval_history DROP CONSTRAINT restaurant_approval_history_service_type_check;
UPDATE restaurant_approval_history SET service_type = 'DINE_IN' WHERE service_type = 'RESTAURANT';
ALTER TABLE restaurant_approval_history ADD CONSTRAINT restaurant_approval_history_service_type_check CHECK (service_type IN ('DINE_IN','TAKEAWAY','DINITICKET'));

-- commission_rules
UPDATE commission_rules SET service_type = 'DINE_IN' WHERE service_type = 'RESTAURANT';

-- promotions
UPDATE promotions SET service_type = 'DINE_IN' WHERE service_type = 'RESTAURANT';
