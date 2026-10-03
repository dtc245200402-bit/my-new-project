package com.oms.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Shipment {

    public enum Status {
        READY("Chờ giao"),
        DELIVERED("Giao thành công"),
        FAILED_RETURNED("Giao không thành công - đã quay về kho");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private long id;
    private String code;
    private long warehouseId;
    private String warehouseName;
    private long customerId;
    private String customerName;
    private Status status;
    private String recipientName;
    private LocalDateTime deliveredAt;
    private String proofImagePath;
    private String failureReason;
    private LocalDateTime returnedAt;
    private LocalDateTime invoiceEligibleAt;
    private List<ShipmentItem> items = new ArrayList<>();

    public boolean isReadyForConfirmation() {
        return status == Status.READY;
    }

    public boolean isInvoiceEligible() {
        return status == Status.DELIVERED && invoiceEligibleAt != null;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(long warehouseId) { this.warehouseId = warehouseId; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public String getProofImagePath() { return proofImagePath; }
    public void setProofImagePath(String proofImagePath) { this.proofImagePath = proofImagePath; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }
    public LocalDateTime getInvoiceEligibleAt() { return invoiceEligibleAt; }
    public void setInvoiceEligibleAt(LocalDateTime invoiceEligibleAt) { this.invoiceEligibleAt = invoiceEligibleAt; }
    public List<ShipmentItem> getItems() { return items; }
    public void setItems(List<ShipmentItem> items) { this.items = items; }
}
