SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_get_users;
DELIMITER //
CREATE PROCEDURE sp_get_users()
BEGIN
    SELECT id, username, password_hash, full_name, role, is_active
    FROM users
    ORDER BY role, full_name;
END //
DELIMITER ;
