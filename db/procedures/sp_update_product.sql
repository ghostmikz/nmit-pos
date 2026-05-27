SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_update_product;
DELIMITER //
CREATE PROCEDURE sp_update_product(
    IN p_id            INT,
    IN p_barcode       VARCHAR(50),
    IN p_name          VARCHAR(150),
    IN p_category_id   INT,
    IN p_price         DECIMAL(10,2),
    IN p_cost_price    DECIMAL(10,2),
    IN p_unit          VARCHAR(20),
    IN p_expiry_date   DATE,
    IN p_low_stock_alert INT
)
BEGIN
    UPDATE products SET
        barcode          = NULLIF(p_barcode, ''),
        name             = p_name,
        category_id      = NULLIF(p_category_id, 0),
        price            = p_price,
        cost_price       = p_cost_price,
        unit             = p_unit,
        expiry_date      = p_expiry_date,
        low_stock_alert  = p_low_stock_alert
    WHERE id = p_id;
    SELECT ROW_COUNT() AS rows_affected;
END//
DELIMITER ;
