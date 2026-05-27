SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_delete_category;
DELIMITER //
CREATE PROCEDURE sp_delete_category(IN p_id INT)
BEGIN
    DECLARE prod_count INT DEFAULT 0;
    SELECT COUNT(*) INTO prod_count FROM products WHERE category_id = p_id AND is_active = TRUE;
    IF prod_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Category has active products';
    END IF;
    DELETE FROM categories WHERE id = p_id;
    SELECT ROW_COUNT() AS rows_affected;
END//
DELIMITER ;
