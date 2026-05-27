SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_get_products;
DELIMITER //
CREATE PROCEDURE sp_get_products()
BEGIN
    SELECT id, barcode, product_name, category_id, category_name, price, cost_price,
           stock_quantity, unit, expiry_date, is_active, has_image, low_stock_alert
    FROM view_product_stock
    ORDER BY product_name;
END //
DELIMITER ;
