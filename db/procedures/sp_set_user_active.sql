DELIMITER $$

CREATE PROCEDURE sp_set_user_active(IN p_id INT, IN p_active TINYINT(1))
BEGIN
    UPDATE users SET is_active = p_active WHERE id = p_id;
END$$

DELIMITER ;
