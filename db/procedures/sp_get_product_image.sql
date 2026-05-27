SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_get_product_image;
DELIMITER //
CREATE PROCEDURE sp_get_product_image(IN p_id INT)
BEGIN
    SELECT image FROM products WHERE id = p_id;
END //
DELIMITER ;
