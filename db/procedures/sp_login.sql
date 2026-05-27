SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_login;

DELIMITER $$

CREATE PROCEDURE sp_login(IN p_username VARCHAR(100))
BEGIN
    UPDATE users SET last_login = NOW()
    WHERE username = p_username AND is_active = 1;

    SELECT id, username, password_hash, full_name, role, is_active
    FROM   users
    WHERE  username  = p_username
      AND  is_active = 1;
END$$

DELIMITER ;
