DELIMITER $$

CREATE PROCEDURE sp_add_user(
    IN p_username     VARCHAR(50),
    IN p_password     VARCHAR(255),
    IN p_full_name    VARCHAR(100),
    IN p_role         VARCHAR(20),
    IN p_created_by   INT
)
BEGIN
    INSERT INTO users (username, password_hash, full_name, role, is_active, created_by)
    VALUES (p_username, p_password, p_full_name, p_role, 1, p_created_by);
    SELECT LAST_INSERT_ID() AS user_id;
END$$

DELIMITER ;
