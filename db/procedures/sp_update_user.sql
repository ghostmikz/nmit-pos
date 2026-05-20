DELIMITER $$

CREATE PROCEDURE sp_update_user(
    IN p_id           INT,
    IN p_full_name    VARCHAR(100),
    IN p_role         VARCHAR(20),
    IN p_password     VARCHAR(255)   -- NULL means keep existing password
)
BEGIN
    IF p_password IS NOT NULL AND p_password != '' THEN
        UPDATE users SET full_name = p_full_name, role = p_role, password_hash = p_password WHERE id = p_id;
    ELSE
        UPDATE users SET full_name = p_full_name, role = p_role WHERE id = p_id;
    END IF;
END$$

DELIMITER ;
