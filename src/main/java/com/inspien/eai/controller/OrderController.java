package com.inspien.eai.controller;

import com.inspien.eai.domain.OrderEntity;
import com.inspien.eai.service.OrderService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    public static void startServer() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/order", new OrderHandler());
        server.start();
        logger.info("Order API Started: http://localhost:8080/order");
    }

    static class OrderHandler implements HttpHandler {

        private static final Logger logger = LoggerFactory.getLogger(OrderHandler.class);
        private final OrderService orderService = new OrderService();

        @Override
        public void handle(HttpExchange exchange) {
            // 요청 단위 traceId 생성
            String traceId = UUID.randomUUID().toString().substring(0, 8);
            MDC.put("traceId", traceId);

            try {
                logger.info("요청 수신 method={}, path={}", exchange.getRequestMethod(), exchange.getRequestURI());

                if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                    logger.warn("잘못된 메서드 요청: {}", exchange.getRequestMethod());
                    json(exchange, 405, "{\"result\":\"FAIL\",\"message\":\"POST only\"}");
                    return;
                }

                // charset 파악
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                Charset charset = resolveCharset(contentType);

                // 요청 바디 읽기
                byte[] raw = exchange.getRequestBody().readAllBytes();
                String xml = new String(raw, charset);

                logger.info("요청 XML 수신 (len={}, charset={})", xml.length(), charset.name());

                // XML 파싱
                Document doc = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(new ByteArrayInputStream(xml.getBytes(charset)));

                doc.getDocumentElement().normalize();

                // 루트 체크
                String rootName = doc.getDocumentElement().getNodeName();
                if (!"ORDERS".equals(rootName)) {
                    //logger.warn("루트 태그 오류: {}", rootName);
                    json(exchange, 400, "{\"result\":\"FAIL\",\"message\":\"Root tag must be <ORDERS>\"}");
                    return;
                }

                NodeList headerNodes = doc.getElementsByTagName("HEADER");
                NodeList itemNodes = doc.getElementsByTagName("ITEM");

                if (headerNodes.getLength() == 0 && itemNodes.getLength() == 0) {
                    //logger.warn("HEADER/ITEM 모두 없음");
                    json(exchange, 400, "{\"result\":\"FAIL\",\"message\":\"No HEADER/ITEM found\"}");
                    return;
                }

                // ====== 스킵 통계 ======
                int invalidHeader = 0;        // HEADER 필수값 누락/이상
                int invalidItem = 0;          // ITEM 필수값 누락/이상
                int headerWithoutItem = 0;    // HEADER는 있는데 ITEM 없음
                int noHeaderForItem = 0;      // ITEM은 있는데 HEADER 없음(개수는 ITEM 건수 기준)

                // ====== 1) HEADER Map: USER_ID -> HeaderInfo (유효한 헤더만 담기) ======
                Map<String, HeaderInfo> headerMap = new LinkedHashMap<>();
                for (int i = 0; i < headerNodes.getLength(); i++) {
                    Element h = (Element) headerNodes.item(i);

                    String userId = text(h, "USER_ID").trim();
                    String name = text(h, "NAME").trim();
                    String address = text(h, "ADDRESS").trim();
                    String status = text(h, "STATUS").trim(); // 공백/줄바꿈 포함 가능

                    // 필수값 검증 (누락이면 스킵)
                    if (isBlank(userId) || isBlank(name) || isBlank(address)) {
                        invalidHeader++;
                        //logger.warn("HEADER 스킵(필수값 누락) userId={}, name={}, address={}", userId, name, address);
                        continue;
                    }

                    // N이 아니면 스킵 처리
                    if (!"N".equals(status)) {
                        invalidHeader++;
                        //logger.warn("HEADER 스킵(STATUS!=N) userId={}, status={}", userId, status);
                        continue;
                    }

                    headerMap.put(userId, new HeaderInfo(userId, name, address, status));
                }

                // ====== 2) ITEM Map: USER_ID -> List<ItemInfo> (유효한 아이템만 담기) ======
                Map<String, List<ItemInfo>> itemsByUser = new HashMap<>();
                for (int i = 0; i < itemNodes.getLength(); i++) {
                    Element it = (Element) itemNodes.item(i);

                    String userId = text(it, "USER_ID").trim();
                    String itemId = text(it, "ITEM_ID").trim();
                    String itemName = text(it, "ITEM_NAME").trim();
                    String priceStr = text(it, "PRICE").trim();

                    if (isBlank(userId) || isBlank(itemId) || isBlank(itemName) || isBlank(priceStr)) {
                        invalidItem++;
                        //logger.warn("ITEM 스킵(필수값 누락) userId={}, itemId={}, itemName={}, price={}", userId, itemId, itemName, priceStr);
                        continue;
                    }

                    int price;
                    try {
                        price = Integer.parseInt(priceStr);
                    } catch (NumberFormatException nfe) {
                        invalidItem++;
                        //logger.warn("ITEM 스킵(PRICE 숫자 아님) priceStr={}", priceStr);
                        continue;
                    }

                    itemsByUser.computeIfAbsent(userId, k -> new ArrayList<>())
                            .add(new ItemInfo(userId, itemId, itemName, price));
                }

                // ====== 3) 매칭/플랫화: "HEADER가 있고 + 해당 USER_ID ITEM이 있는 것만 적재" ======
                List<OrderEntity> flatRows = new ArrayList<>();

                // 헤더 기준으로: 아이템 없으면 headerWithoutItem 증가, 있으면 row 생성
                for (HeaderInfo h : headerMap.values()) {
                    List<ItemInfo> its = itemsByUser.get(h.userId);
                    if (its == null || its.isEmpty()) {
                        headerWithoutItem++;
                        logger.warn("HEADER 스킵(ITEM 없음) userId={}", h.userId);
                        continue;
                    }

                    for (ItemInfo it : its) {
                        OrderEntity row = new OrderEntity();
                        row.setUserId(h.userId);
                        row.setName(h.name);
                        row.setAddress(h.address);
                        row.setItemId(it.itemId);
                        row.setItemName(it.itemName);

                        // ORDER_TB PRICE 컬럼이 VARCHAR2(100)이므로 String으로 세팅
                        row.setPrice(String.valueOf(it.price));

                        flatRows.add(row);
                    }
                }

                // 헤더 없는 아이템 개수 카운트
                for (Map.Entry<String, List<ItemInfo>> e : itemsByUser.entrySet()) {
                    if (!headerMap.containsKey(e.getKey())) {
                        noHeaderForItem += e.getValue().size();
                        logger.warn("ITEM 스킵(HEADER 없음) userId={}, count={}", e.getKey(), e.getValue().size());
                    }
                }

                logger.info("검증/매칭 결과 headers(valid)={}, items(valid)={}, flatRows(valid)={}, skipped={}",
                        headerMap.size(), itemsByUser.values().stream().mapToInt(List::size).sum(),
                        flatRows.size(),
                        String.format("{invalidHeader=%d, invalidItem=%d, headerWithoutItem=%d, noHeaderForItem=%d}",
                                invalidHeader, invalidItem, headerWithoutItem, noHeaderForItem));

                // 유효 row가 하나도 없으면 FAIL (과제: SYNC 성공/실패 응답)
                if (flatRows.isEmpty()) {
                    String body =
                            "{"
                                    + "\"result\":\"FAIL\","
                                    + "\"message\":\"No valid orders to process\","
                                    + "\"insertedRows\":0,"
                                    + "\"skipped\":{"
                                    + "\"invalidHeader\":" + invalidHeader + ","
                                    + "\"invalidItem\":" + invalidItem + ","
                                    + "\"headerWithoutItem\":" + headerWithoutItem + ","
                                    + "\"noHeaderForItem\":" + noHeaderForItem
                                    + "}"
                                    + "}";
                    json(exchange, 400, body);
                    return;
                }

                // ====== 4) Service 실행 (DB + SFTP / 트랜잭션) ======
                String orderId = orderService.createOrders(flatRows);

                logger.info("주문 처리 성공 orderId={}, insertedRows={}", orderId, flatRows.size());

                String body =
                        "{"
                                + "\"result\":\"SUCCESS\","
                                + "\"message\":\"processed\","
                                + "\"orderId\":\"" + orderId + "\","
                                + "\"insertedRows\":" + flatRows.size() + ","
                                + "\"skipped\":{"
                                + "\"invalidHeader\":" + invalidHeader + ","
                                + "\"invalidItem\":" + invalidItem + ","
                                + "\"headerWithoutItem\":" + headerWithoutItem + ","
                                + "\"noHeaderForItem\":" + noHeaderForItem
                                + "}"
                                + "}";
                json(exchange, 200, body);

            } catch (Exception e) {
                logger.error("주문 처리 실패", e);
                try {
                    json(exchange, 500, "{\"result\":\"FAIL\",\"message\":\"Internal Error\"}");
                } catch (Exception ignored) {
                }
            } finally {
                MDC.remove("traceId");
            }
        }

        private static Charset resolveCharset(String contentType) {
            if (contentType == null) return StandardCharsets.UTF_8;
            String lower = contentType.toLowerCase(Locale.ROOT);
            int idx = lower.indexOf("charset=");
            if (idx < 0) return StandardCharsets.UTF_8;

            String cs = lower.substring(idx + "charset=".length()).trim();
            if (cs.contains(";")) cs = cs.substring(0, cs.indexOf(";")).trim();

            try {
                return Charset.forName(cs);
            } catch (Exception e) {
                return StandardCharsets.UTF_8;
            }
        }

        private static String text(Element parent, String tag) {
            Node n = parent.getElementsByTagName(tag).item(0);
            return n == null ? "" : n.getTextContent();
        }

        private static boolean isBlank(String s) {
            return s == null || s.trim().isEmpty();
        }

        private void json(HttpExchange ex, int status, String body) throws Exception {
            byte[] res = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            ex.sendResponseHeaders(status, res.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(res);
            }
        }
    }

    static class HeaderInfo {
        final String userId, name, address, status;

        HeaderInfo(String userId, String name, String address, String status) {
            this.userId = userId;
            this.name = name;
            this.address = address;
            this.status = status;
        }
    }

    static class ItemInfo {
        final String userId, itemId, itemName;
        final int price;

        ItemInfo(String userId, String itemId, String itemName, int price) {
            this.userId = userId;
            this.itemId = itemId;
            this.itemName = itemName;
            this.price = price;
        }
    }
}
