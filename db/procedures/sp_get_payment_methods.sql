SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS sp_get_payment_methods;
DELIMITER //
CREATE PROCEDURE sp_get_payment_methods()
BEGIN
    SELECT id, name FROM payment_methods ORDER BY id;
END//
DELIMITER ;
