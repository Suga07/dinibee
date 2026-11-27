INSERT INTO permissions(id,code,description) VALUES
(md5('permission:RESERVATION_MANAGE')::uuid,'RESERVATION_MANAGE','Manage reservations of owned restaurants'),
(md5('permission:ORDER_MANAGE')::uuid,'ORDER_MANAGE','Manage orders of owned restaurants'),
(md5('permission:DINITICKET_MANAGE')::uuid,'DINITICKET_MANAGE','Manage queues of owned restaurants');
INSERT INTO role_permissions(role_id,permission_id) SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE r.code IN ('OWNER','SUPER_ADMIN') AND p.code IN ('RESERVATION_MANAGE','ORDER_MANAGE','DINITICKET_MANAGE');
