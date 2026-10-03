package com.oms.model;

import java.math.BigDecimal;

public class ShipmentItem {

    private long id;
    private long shipmentId;
    private long productId;
    private String productSku;
    private String productName;
    private long unitId;
    private String unitName;
    private BigDecimal quantity;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getShipmentId() { return shipmentId; }
    public void setShipmentId(long shipmentId) { this.shipmentId = shipmentId; }
    public long getProductId() { return productId; }
    public void setProductId(long productId) { this.productId = productId; }
    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public long getUnitId() { return unitId; }
    public void setUnitId(long unitId) { this.unitId = unitId; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}
