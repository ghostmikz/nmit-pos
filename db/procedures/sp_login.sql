DROP PROCEDURE IF EXISTS sp_login;

DELIMITER $$

CREATE PROCEDURE sp_login(IN p_username VARCHAR(100))
BEGIN
    SELECT id, username, password_hash, full_name, role
    FROM   users
    WHERE  username  = p_username
      AND  is_active = 1;
END$$

DELIMITER ;
