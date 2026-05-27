SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_add_product;
DELIMITER //
CREATE PROCEDURE sp_add_product(
    IN p_barcode       VARCHAR(50),
    IN p_name          VARCHAR(150),
    IN p_category_id   INT,
    IN p_price         DECIMAL(10,2),
    IN p_cost_price    DECIMAL(10,2),
    IN p_stock_quantity INT,
    IN p_unit          VARCHAR(20),
    IN p_expiry_date   DATE,
    IN p_low_stock_alert INT
)
BEGIN
    INSERT INTO products (barcode, name, category_id, price, cost_price, stock_quantity, unit, expiry_date, low_stock_alert)
    VALUES (NULLIF(p_barcode, ''), p_name, NULLIF(p_category_id, 0), p_price, p_cost_price,
            p_stock_quantity, p_unit, p_expiry_date, p_low_stock_alert);
    SELECT LAST_INSERT_ID() AS product_id;
END//
DELIMITER ;
