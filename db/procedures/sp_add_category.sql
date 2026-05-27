SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_add_category;
DELIMITER //
CREATE PROCEDURE sp_add_category(IN p_name VARCHAR(100))
BEGIN
    INSERT INTO categories (name) VALUES (p_name);
    SELECT LAST_INSERT_ID() AS category_id, p_name AS name;
END//
DELIMITER ;
