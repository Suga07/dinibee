INSERT INTO permissions(id,code,description) VALUES
(md5('permission:TABLE_MANAGE')::uuid,'TABLE_MANAGE','Manage tables of owned restaurants'),
(md5('permission:MENU_MANAGE')::uuid,'MENU_MANAGE','Manage menus of owned restaurants');
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('OWNER','SUPER_ADMIN') AND p.code IN ('TABLE_MANAGE','MENU_MANAGE');
