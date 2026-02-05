package com.inspien.eai.domain;

public class ShipmentEntity {

    private String shipmentId;
    private String orderId;
    private String itemId;
    private String applicantKey;
    private String address;

    public String getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getApplicantKey() {
        return applicantKey;
    }

    public void setApplicantKey(String applicantKey) {
        this.applicantKey = applicantKey;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
