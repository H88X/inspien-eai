package com.inspien.eai.util;

public class OrderIdGenerator {

    public static String next(String currentOrMaxId) {

        if (currentOrMaxId == null || currentOrMaxId.isBlank()) {
            return "M001";
        }

        if (!currentOrMaxId.startsWith("M") || currentOrMaxId.length() < 2) {
            return "M001";
        }

        int n;
        try {
            n = Integer.parseInt(currentOrMaxId.substring(1));
        } catch (NumberFormatException e) {
            return "M001";
        }

        n++;

        if (n > 999) {
            throw new IllegalStateException("ORDER_ID 범위 초과 (M999 초과)");
        }

        return String.format("M%03d", n);
    }
}
