package com.oms.dao;

import com.oms.model.Shipment;
import com.oms.model.ShipmentItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ShipmentDao {

    private static final String COLUMNS = """
            s.id, s.code, s.warehouse_id, w.name AS warehouse_name,
            s.customer_id, c.name AS customer_name, s.status,
            s.recipient_name, s.delivered_at, s.proof_image_path,
            s.failure_reason, s.returned_at, s.invoice_eligible_at
            """;

    public Shipment findById(Connection conn, long id) throws SQLException {
        return findOne(conn, "SELECT " + COLUMNS + " FROM shipments s "
                + "JOIN warehouses w ON w.id = s.warehouse_id "
                + "JOIN customers c ON c.id = s.customer_id "
                + "WHERE s.id = ?", id);
    }

    public Shipment findByIdForUpdate(Connection conn, long id) throws SQLException {
        return findOne(conn, "SELECT " + COLUMNS + " FROM shipments s "
                + "JOIN warehouses w ON w.id = s.warehouse_id "
                + "JOIN customers c ON c.id = s.customer_id "
                + "WHERE s.id = ? FOR UPDATE", id);
    }

    public List<ShipmentItem> findItems(Connection conn, long shipmentId) throws SQLException {
        String sql = """
                SELECT si.id, si.shipment_id, si.product_id, p.sku, p.name,
                       si.unit_id, u.name AS unit_name, si.quantity
                FROM shipment_items si
                JOIN products p ON p.id = si.product_id
                JOIN units u ON u.id = si.unit_id
                WHERE si.shipment_id = ?
                ORDER BY si.id
                """;
        List<ShipmentItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, shipmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ShipmentItem item = new ShipmentItem();
                    item.setId(rs.getLong("id"));
                    item.setShipmentId(rs.getLong("shipment_id"));
                    item.setProductId(rs.getLong("product_id"));
                    item.setProductSku(rs.getString("sku"));
                    item.setProductName(rs.getString("name"));
                    item.setUnitId(rs.getLong("unit_id"));
                    item.setUnitName(rs.getString("unit_name"));
                    item.setQuantity(rs.getBigDecimal("quantity"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    public int markDelivered(Connection conn, long shipmentId, String recipientName,
                             LocalDateTime deliveredAt, String proofImagePath,
                             LocalDateTime invoiceEligibleAt) throws SQLException {
        String sql = """
                UPDATE shipments
                SET status = 'DELIVERED',
                    recipient_name = ?,
                    delivered_at = ?,
                    proof_image_path = ?,
                    failure_reason = NULL,
                    returned_at = NULL,
                    invoice_eligible_at = ?
                WHERE id = ? AND status = 'READY'
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, recipientName);
            ps.setTimestamp(2, Timestamp.valueOf(deliveredAt));
            ps.setString(3, proofImagePath);
            ps.setTimestamp(4, Timestamp.valueOf(invoiceEligibleAt));
            ps.setLong(5, shipmentId);
            return ps.executeUpdate();
        }
    }

    public int markFailedReturned(Connection conn, long shipmentId, String failureReason,
                                  LocalDateTime returnedAt) throws SQLException {
        String sql = """
                UPDATE shipments
                SET status = 'FAILED_RETURNED',
                    failure_reason = ?,
                    returned_at = ?,
                    recipient_name = NULL,
                    delivered_at = NULL,
                    proof_image_path = NULL,
                    invoice_eligible_at = NULL
                WHERE id = ? AND status = 'READY'
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, failureReason);
            ps.setTimestamp(2, Timestamp.valueOf(returnedAt));
            ps.setLong(3, shipmentId);
            return ps.executeUpdate();
        }
    }

    public long insertReturnMovement(Connection conn, long warehouseId, ShipmentItem item,
                                     long actorId, long shipmentId) throws SQLException {
        String sql = """
                INSERT INTO inventory_movements
                    (warehouse_id, product_id, unit_id, quantity,
                     movement_type, reference_type, reference_id, created_by)
                VALUES (?, ?, ?, ?, 'SHIPMENT_RETURN', 'SHIPMENT', ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, warehouseId);
            ps.setLong(2, item.getProductId());
            ps.setLong(3, item.getUnitId());
            ps.setBigDecimal(4, item.getQuantity());
            ps.setLong(5, shipmentId);
            ps.setLong(6, actorId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không lấy được ID bút toán nhập lại kho.");
                }
                return keys.getLong(1);
            }
        }
    }

    public boolean userCanAccessWarehouse(Connection conn, long userId, long warehouseId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM user_warehouses WHERE user_id = ? AND warehouse_id = ?")) {
            ps.setLong(1, userId);
            ps.setLong(2, warehouseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Shipment findOne(Connection conn, String sql, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return map(rs);
            }
        }
    }

    private static Shipment map(ResultSet rs) throws SQLException {
        Shipment s = new Shipment();
        s.setId(rs.getLong("id"));
        s.setCode(rs.getString("code"));
        s.setWarehouseId(rs.getLong("warehouse_id"));
        s.setWarehouseName(rs.getString("warehouse_name"));
        s.setCustomerId(rs.getLong("customer_id"));
        s.setCustomerName(rs.getString("customer_name"));
        s.setStatus(Shipment.Status.valueOf(rs.getString("status")));
        s.setRecipientName(rs.getString("recipient_name"));

        Timestamp deliveredAt = rs.getTimestamp("delivered_at");
        s.setDeliveredAt(deliveredAt == null ? null : deliveredAt.toLocalDateTime());
        s.setProofImagePath(rs.getString("proof_image_path"));
        s.setFailureReason(rs.getString("failure_reason"));

        Timestamp returnedAt = rs.getTimestamp("returned_at");
        s.setReturnedAt(returnedAt == null ? null : returnedAt.toLocalDateTime());

        Timestamp eligibleAt = rs.getTimestamp("invoice_eligible_at");
        s.setInvoiceEligibleAt(eligibleAt == null ? null : eligibleAt.toLocalDateTime());
        return s;
    }
}
