-- Reconcile owner menu privileges when a deployment has an incomplete role_permissions seed.
-- Does not add user roles, change restaurant ownership, or open endpoints publicly.
INSERT INTO permissions (id, code, description)
VALUES (md5('permission:MENU_MANAGE')::uuid, 'MENU_MANAGE', 'Manage menus of owned restaurants')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r JOIN permissions p ON p.code = 'MENU_MANAGE'
WHERE r.code = 'OWNER'
ON CONFLICT DO NOTHING;
