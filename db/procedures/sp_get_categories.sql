SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_get_categories;
DELIMITER //
CREATE PROCEDURE sp_get_categories()
BEGIN
    SELECT id, name, description FROM categories ORDER BY name;
END //
DELIMITER ;
