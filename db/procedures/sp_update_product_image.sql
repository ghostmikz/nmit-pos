SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_update_product_image;
DELIMITER //
CREATE PROCEDURE sp_update_product_image(IN p_id INT, IN p_image LONGBLOB)
BEGIN
    UPDATE products SET image = p_image WHERE id = p_id;
END //
DELIMITER ;
