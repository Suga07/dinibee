-- Reconcile owner permissions when a production deployment has an incomplete role_permissions seed.
-- Ensures OWNER and RESTAURANT_MANAGER roles have all operational authorities.

-- 1. Ensure permissions exist
INSERT INTO permissions (id, code, description) VALUES
(md5('permission:DINITICKET_MANAGE')::uuid, 'DINITICKET_MANAGE', 'Manage queues of owned restaurants'),
(md5('permission:RESERVATION_MANAGE')::uuid, 'RESERVATION_MANAGE', 'Manage reservations of owned restaurants'),
(md5('permission:ORDER_MANAGE')::uuid, 'ORDER_MANAGE', 'Manage orders of owned restaurants'),
(md5('permission:TABLE_MANAGE')::uuid, 'TABLE_MANAGE', 'Manage tables of owned restaurants'),
(md5('permission:MENU_MANAGE')::uuid, 'MENU_MANAGE', 'Manage menus of owned restaurants'),
(md5('permission:DINING_MANAGE')::uuid, 'DINING_MANAGE', 'Dining Manage'),
(md5('permission:OWNER_REPORT_VIEW')::uuid, 'OWNER_REPORT_VIEW', 'Owner Report View'),
(md5('permission:OWNER_PAYMENT_VIEW')::uuid, 'OWNER_PAYMENT_VIEW', 'Owner Payment View'),
(md5('permission:OWNER_PAYMENT_MANAGE')::uuid, 'OWNER_PAYMENT_MANAGE', 'Confirm owned restaurant offline payments and refunds'),
(md5('permission:SUPPORT_OWN')::uuid, 'SUPPORT_OWN', 'Support Own')
ON CONFLICT (code) DO NOTHING;

-- 2. Link all operational permissions to OWNER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'OWNER'
  AND p.code IN (
    'DINITICKET_MANAGE',
    'RESERVATION_MANAGE',
    'ORDER_MANAGE',
    'TABLE_MANAGE',
    'MENU_MANAGE',
    'DINING_MANAGE',
    'OWNER_REPORT_VIEW',
    'OWNER_PAYMENT_VIEW',
    'OWNER_PAYMENT_MANAGE',
    'SUPPORT_OWN',
    'RESTAURANT_OWN_MANAGE',
    'OWNER_SELF_READ',
    'RESTAURANT_CREATE'
  )
ON CONFLICT DO NOTHING;

-- 3. Link queue and operational permissions to RESTAURANT_MANAGER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'RESTAURANT_MANAGER'
  AND p.code IN (
    'DINITICKET_MANAGE',
    'RESERVATION_MANAGE',
    'ORDER_MANAGE',
    'TABLE_MANAGE',
    'MENU_MANAGE',
    'DINING_MANAGE',
    'OWNER_REPORT_VIEW',
    'RESTAURANT_OWN_MANAGE'
  )
ON CONFLICT DO NOTHING;
