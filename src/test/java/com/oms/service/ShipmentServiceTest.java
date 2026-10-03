package com.oms.service;

import com.oms.model.Role;
import com.oms.model.Shipment;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ShipmentServiceTest extends DatabaseTest {

    @TempDir
    Path uploads;

    @BeforeEach
    void seedShipmentCatalog() throws Exception {
        sql("INSERT INTO products (id, sku, name, category_id, base_unit_id, status) VALUES "
                + "(500, 'COCA', 'Coca lon', 3, 1, 'ACTIVE'), "
                + "(501, 'HAOHAO', 'Mì Hảo Hảo', 3, 3, 'ACTIVE')");
    }

    @Test
    void confirmsSuccessfulDeliveryWithSignedProofAndEnablesInvoice() throws Exception {
        User warehouseStaff = warehouseUser("nvkho01", 10);
        long shipmentId = shipment(1001, "PX-1001");

        ShipmentService service = new ShipmentService(new FileStorage(uploads));
        service.confirmSuccess(shipmentId, "Nguyễn Văn B", image(), warehouseStaff);

        Shipment result = service.get(shipmentId);
        assertEquals(Shipment.Status.DELIVERED, result.getStatus());
        assertEquals("Nguyễn Văn B", result.getRecipientName());
        assertNotNull(result.getDeliveredAt());
        assertNotNull(result.getProofImagePath());
        assertTrue(result.isInvoiceEligible());
        assertTrue(Files.isRegularFile(uploads.resolve(result.getProofImagePath())));
        assertEquals(1, count("audit_logs", "action = 'SHIPMENT_DELIVERED'"));
        assertEquals(0, count("inventory_movements", "1 = 1"));
    }

    @Test
    void failedDeliveryRequiresReasonAndCreatesReturnMovementsInSameTransaction() throws Exception {
        User warehouseStaff = warehouseUser("nvkho02", 10);
        long shipmentId = shipment(1002, "PX-1002");
        sql("INSERT INTO shipment_items (shipment_id, product_id, unit_id, quantity) VALUES ("
                + shipmentId + ", 500, 1, 24.0000), ("
                + shipmentId + ", 501, 3, 10.0000)");

        ShipmentService service = new ShipmentService(new FileStorage(uploads));
        service.confirmFailure(shipmentId, "Đại lý từ chối nhận hàng", warehouseStaff);

        Shipment result = service.get(shipmentId);
        assertEquals(Shipment.Status.FAILED_RETURNED, result.getStatus());
        assertEquals("Đại lý từ chối nhận hàng", result.getFailureReason());
        assertNotNull(result.getReturnedAt());
        assertFalse(result.isInvoiceEligible());
        assertEquals(2, count("inventory_movements", "reference_type = 'SHIPMENT' AND reference_id = " + shipmentId));
        assertEquals(1, count("audit_logs", "action = 'SHIPMENT_FAILED_RETURNED'"));
    }

    @Test
    void rejectsSuccessWithoutRecipientOrProofAndFailureWithoutReason() throws Exception {
        User warehouseStaff = warehouseUser("nvkho03", 10);
        long first = shipment(1003, "PX-1003");
        long second = shipment(1004, "PX-1004");
        ShipmentService service = new ShipmentService(new FileStorage(uploads));

        assertThrows(ServiceException.class, () -> service.confirmSuccess(first, "", image(), warehouseStaff));
        assertThrows(ServiceException.class, () -> service.confirmSuccess(first, "Người nhận", new byte[0], warehouseStaff));
        assertThrows(ServiceException.class, () -> service.confirmFailure(second, " ", warehouseStaff));
    }

    @Test
    void warehouseScopeAndFinalStateAreEnforcedByService() throws Exception {
        User warehouseStaff = warehouseUser("nvkho04", 10);
        User otherWarehouseStaff = warehouseUser("nvkho05", 20);
        long shipmentId = shipment(1005, "PX-1005");

        ShipmentService service = new ShipmentService(new FileStorage(uploads));
        assertThrows(ServiceException.class,
                () -> service.confirmSuccess(shipmentId, "Người nhận", image(), otherWarehouseStaff));

        service.confirmSuccess(shipmentId, "Người nhận", image(), warehouseStaff);
        assertThrows(ServiceException.class,
                () -> service.confirmFailure(shipmentId, "Đại lý không nhận", warehouseStaff));
    }

    private User warehouseUser(String username, long warehouseId) throws Exception {
        User user = user(username, Role.WAREHOUSE);
        sql("INSERT INTO user_warehouses (user_id, warehouse_id) VALUES (" + user.getId() + ", " + warehouseId + ")");
        return reload(user.getId());
    }

    private long shipment(long customerId, String code) throws Exception {
        sql("INSERT INTO customers (id, code, name) VALUES (" + customerId + ", 'DL" + customerId + "', 'Đại lý " + customerId + "')");
        sql("INSERT INTO shipments (id, code, warehouse_id, customer_id) VALUES (" + customerId + ", '" + code + "', 10, " + customerId + ")");
        return customerId;
    }

    private static byte[] image() throws Exception {
        BufferedImage img = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return out.toByteArray();
    }
}
