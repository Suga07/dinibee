CREATE OR REPLACE VIEW user_role_summary AS
SELECT
    u.id AS user_id,
    u.first_name,
    u.last_name,
    u.email,
    u.status,
    COALESCE(string_agg(r.code::text, ', ' ORDER BY r.code), '') AS roles
FROM users u
LEFT JOIN user_roles ur ON ur.user_id = u.id
LEFT JOIN roles r ON r.id = ur.role_id
GROUP BY u.id, u.first_name, u.last_name, u.email, u.status;
