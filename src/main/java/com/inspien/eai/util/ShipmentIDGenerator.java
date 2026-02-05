package com.inspien.eai.util;

public class ShipmentIDGenerator {

    public static String generate(String maxShipmentId) {

        if (maxShipmentId == null || maxShipmentId.trim().isEmpty()) {
            return "K001";
        }

        // K가 아닌 값이 섞여오면 예외 처리
        if (!maxShipmentId.startsWith("K") || maxShipmentId.length() < 4) {
            throw new IllegalArgumentException("잘못된 SHIPMENT_ID 형식: " + maxShipmentId);
        }

        int number;
        try {
            number = Integer.parseInt(maxShipmentId.substring(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("SHIPMENT_ID 숫자 파싱 실패: " + maxShipmentId, e);
        }

        number++;

        if (number > 999) {
            throw new IllegalStateException("SHIPMENT_ID 범위 초과");
        }

        return String.format("K%03d", number);
    }
}
