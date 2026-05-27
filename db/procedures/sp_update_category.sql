SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_update_category;
DELIMITER //
CREATE PROCEDURE sp_update_category(IN p_id INT, IN p_name VARCHAR(100))
BEGIN
    UPDATE categories SET name = p_name WHERE id = p_id;
    SELECT ROW_COUNT() AS rows_affected;
END//
DELIMITER ;
