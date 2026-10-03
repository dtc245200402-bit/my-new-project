-- S6-08: xác nhận giao hàng và bằng chứng giao nhận.
-- Chạy sau schema/migration hiện có. Phần tạo đơn hàng thuộc các sprint trước/sau;
-- shipment là bản ghi bàn giao hàng cho module SHIPMENT.

USE oms;

CREATE TABLE shipments (
  id                  BIGINT       NOT NULL AUTO_INCREMENT,
  code                VARCHAR(30)  NOT NULL,
  warehouse_id        BIGINT       NOT NULL,
  customer_id         BIGINT       NOT NULL,
  status              ENUM('READY','DELIVERED','FAILED_RETURNED') NOT NULL DEFAULT 'READY',
  recipient_name      VARCHAR(200) NULL,
  delivered_at        DATETIME     NULL,
  proof_image_path    VARCHAR(255) NULL,
  failure_reason      VARCHAR(500) NULL,
  returned_at         DATETIME     NULL,
  invoice_eligible_at DATETIME     NULL,
  created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_shipments_code (code),
  KEY idx_shipments_warehouse_status (warehouse_id, status),
  KEY idx_shipments_customer (customer_id),
  KEY idx_shipments_invoice_eligible (invoice_eligible_at),
  CONSTRAINT fk_shipments_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
  CONSTRAINT fk_shipments_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE shipment_items (
  id          BIGINT         NOT NULL AUTO_INCREMENT,
  shipment_id BIGINT         NOT NULL,
  product_id  BIGINT         NOT NULL,
  unit_id     BIGINT         NOT NULL,
  quantity    DECIMAL(15,4)  NOT NULL,
  PRIMARY KEY (id),
  KEY idx_shipment_items_shipment (shipment_id),
  KEY idx_shipment_items_product (product_id),
  CONSTRAINT fk_shipment_items_shipment FOREIGN KEY (shipment_id) REFERENCES shipments (id) ON DELETE CASCADE,
  CONSTRAINT fk_shipment_items_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT fk_shipment_items_unit FOREIGN KEY (unit_id) REFERENCES units (id),
  CONSTRAINT chk_shipment_items_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Nhật ký hàng quay về kho khi giao thất bại.
-- Tồn kho thực tế có thể tổng hợp từ các movement của module INVENTORY.
CREATE TABLE inventory_movements (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  warehouse_id   BIGINT        NOT NULL,
  product_id     BIGINT        NOT NULL,
  unit_id        BIGINT        NOT NULL,
  quantity       DECIMAL(15,4) NOT NULL,
  movement_type  ENUM('SHIPMENT_RETURN') NOT NULL,
  reference_type VARCHAR(50)   NOT NULL,
  reference_id   BIGINT        NOT NULL,
  created_by     BIGINT        NOT NULL,
  created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_inventory_movements_warehouse_created (warehouse_id, created_at),
  KEY idx_inventory_movements_reference (reference_type, reference_id),
  CONSTRAINT fk_inventory_movements_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
  CONSTRAINT fk_inventory_movements_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT fk_inventory_movements_unit FOREIGN KEY (unit_id) REFERENCES units (id),
  CONSTRAINT fk_inventory_movements_user FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT chk_inventory_movements_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
