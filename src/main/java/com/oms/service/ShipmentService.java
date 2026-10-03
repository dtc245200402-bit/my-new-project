package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.ShipmentDao;
import com.oms.model.Role;
import com.oms.model.Shipment;
import com.oms.model.User;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ShipmentService {

    private static final String PROOF_FOLDER = "delivery-documents";
    private static final int MAX_RECIPIENT_LENGTH = 200;
    private static final int MAX_FAILURE_REASON_LENGTH = 500;

    private final ShipmentDao shipmentDao = new ShipmentDao();
    private final AuditLogDao auditDao = new AuditLogDao();
    private final FileStorage storage;

    public ShipmentService() {
        this(new FileStorage());
    }

    public ShipmentService(FileStorage storage) {
        this.storage = storage;
    }

    public Shipment get(long id) throws SQLException {
        if (id <= 0) {
            throw new ServiceException("Mã phiếu giao hàng không hợp lệ.");
        }
        try (Connection conn = Db.getConnection()) {
            Shipment shipment = shipmentDao.findById(conn, id);
            if (shipment == null) {
                throw new NotFoundException();
            }
            shipment.setItems(shipmentDao.findItems(conn, id));
            return shipment;
        }
    }

    public void confirmSuccess(long shipmentId, String recipientName, byte[] proofImage, User actor)
            throws SQLException, IOException {
        requireWarehouseActor(actor);
        String recipient = normalizeRequired(recipientName, "Người nhận", MAX_RECIPIENT_LENGTH);
        byte[] proofPng = normalizeProofImage(proofImage);

        String filePath = storage.write(PROOF_FOLDER,
                FileStorage.randomName() + ".png", proofPng);

        LocalDateTime now = LocalDateTime.now();
        try {
            Db.inTransaction(conn -> {
                Shipment shipment = shipmentDao.findByIdForUpdate(conn, shipmentId);
                ensureConfirmable(conn, shipment, actor);

                int updated = shipmentDao.markDelivered(conn, shipmentId, recipient, now, filePath, now);
                if (updated != 1) {
                    throw new ServiceException("Phiếu giao hàng đã được xác nhận trước đó.");
                }

                auditDao.log(conn, actor.getId(), "SHIPMENT_DELIVERED", "SHIPMENT", shipmentId,
                        shipment.getCode() + " / người nhận: " + recipient,
                        describeForAudit(shipment), "DELIVERED");
                return null;
            });
        } catch (SQLException | RuntimeException e) {
            storage.deleteQuietly(filePath);
            throw e;
        }
    }

    public void confirmFailure(long shipmentId, String failureReason, User actor) throws SQLException {
        requireWarehouseActor(actor);
        String reason = normalizeRequired(failureReason, "Lý do giao không thành công",
                MAX_FAILURE_REASON_LENGTH);

        LocalDateTime now = LocalDateTime.now();
        Db.inTransaction(conn -> {
            Shipment shipment = shipmentDao.findByIdForUpdate(conn, shipmentId);
            ensureConfirmable(conn, shipment, actor);

            var items = shipmentDao.findItems(conn, shipmentId);
            if (items.isEmpty()) {
                throw new ServiceException("Phiếu giao hàng chưa có mặt hàng nên không thể ghi nhận hàng quay về kho.");
            }

            int updated = shipmentDao.markFailedReturned(conn, shipmentId, reason, now);
            if (updated != 1) {
                throw new ServiceException("Phiếu giao hàng đã được xác nhận trước đó.");
            }

            for (var item : items) {
                shipmentDao.insertReturnMovement(conn, shipment.getWarehouseId(), item, actor.getId(), shipmentId);
            }

            auditDao.log(conn, actor.getId(), "SHIPMENT_FAILED_RETURNED", "SHIPMENT", shipmentId,
                    shipment.getCode() + " / lý do: " + reason,
                    describeForAudit(shipment), "FAILED_RETURNED");
            return null;
        });
    }

    private void ensureConfirmable(Connection conn, Shipment shipment, User actor) throws SQLException {
        if (shipment == null) {
            throw new NotFoundException();
        }
        if (!shipment.isReadyForConfirmation()) {
            throw new ServiceException("Phiếu giao hàng \"" + shipment.getCode()
                    + "\" đã có trạng thái \"" + shipment.getStatus().getLabel() + "\" và không thể xác nhận lại.");
        }
        if (!actor.isAdmin() && !shipmentDao.userCanAccessWarehouse(conn, actor.getId(), shipment.getWarehouseId())) {
            throw new ServiceException("Bạn không được phép thao tác phiếu giao hàng thuộc kho này.");
        }
    }

    private static void requireWarehouseActor(User actor) {
        if (actor == null || (!actor.isAdmin()
                && !actor.hasRole(Role.WAREHOUSE)
                && !actor.hasRole(Role.WH_MANAGER))) {
            throw new ServiceException("Chỉ Nhân viên kho, Quản lý kho hoặc Quản trị hệ thống được xác nhận giao hàng.");
        }
    }

    private static String normalizeRequired(String raw, String label, int max) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            throw new ServiceException("Vui lòng nhập " + label + ".");
        }
        if (value.length() > max) {
            throw new ServiceException(label + " tối đa " + max + " ký tự.");
        }
        return value;
    }

    private static byte[] normalizeProofImage(byte[] data) {
        BufferedImage image = Images.readJpegOrPng(data);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", out)) {
                throw new ServiceException("Không thể xử lý ảnh biên bản.");
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new ServiceException("Không thể xử lý ảnh biên bản.");
        }
    }

    private static String describeForAudit(Shipment shipment) {
        return "Kho: " + shipment.getWarehouseName()
                + "\nĐại lý: " + shipment.getCustomerName()
                + "\nTrạng thái cũ: " + shipment.getStatus().name();
    }

    public static class NotFoundException extends ServiceException {
        public NotFoundException() {
            super("Không tìm thấy phiếu giao hàng.");
        }
    }
}
