package com.inspien.eai.service;

import com.inspien.eai.common.Constants;
import com.inspien.eai.config.MyBatisConfig;
import com.inspien.eai.domain.OrderEntity;
import com.inspien.eai.mapper.OrderMapper;
import com.inspien.eai.sftp.SftpSender;
import com.inspien.eai.util.OrderIdGenerator;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    /**
     * 시나리오1
     */
    public String createOrders(List<OrderEntity> flatRows) {

        if (flatRows == null || flatRows.isEmpty()) {
            throw new IllegalArgumentException("주문 데이터가 비어있습니다.");
        }

        SqlSession session = null;

        // 응답용 대표 ORDER_ID (첫 row)
        String firstOrderId = null;

        try {
            session = MyBatisConfig.getFactory().openSession(false);
            OrderMapper mapper = session.getMapper(OrderMapper.class);

            // 1) M으로 시작하는 최대 ORDER_ID 조회
            String maxId = mapper.selectMaxOrderId();

            // 2) 첫 번째 신규 ORDER_ID 생성
            String nextId = OrderIdGenerator.next(maxId);

            firstOrderId = nextId;
            MDC.put("orderId", firstOrderId);

            logger.info("주문 처리 시작 firstOrderId={}, rows={}", firstOrderId, flatRows.size());

            // 3) DB Insert (ORDER_TB) - row마다 ORDER_ID 다르게 부여
            for (int i = 0; i < flatRows.size(); i++) {
                OrderEntity row = flatRows.get(i);

                row.setOrderId(nextId); // row별로 다른 ORDER_ID
                row.setApplicantKey(Constants.APPLICANT_KEY);

                // XML에 STATUS가 있어도 배치 기준은 'N' 미전송으로 고정(과제 조건)
                row.setStatus("N");

                int inserted = mapper.insertOrder(row);
                if (inserted != 1) {
                    throw new IllegalStateException(
                            "ORDER_TB insert 실패 userId=" + row.getUserId() + ", itemId=" + row.getItemId()
                    );
                }

                //logger.info("ORDER_TB 적재 성공 rowIndex={}, orderId={}, userId={}, itemId={}", i, row.getOrderId(), row.getUserId(), row.getItemId());

                // 다음 row용 ORDER_ID 발급
                nextId = OrderIdGenerator.next(nextId);
            }
            logger.info("ORDER_TB 적재 성공");
            // 4) 파일명 규칙: INSPIEN_[참여자명(한글)]_[yyyyMMddHHmmss].txt
            String fileName = "INSPIEN_[" + Constants.PARTICIPANT_NAME_KR + "]_[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "].txt";


            // 5) 파일내용 포맷
            String content = buildReceiptContent(flatRows);

            logger.info("영수증 파일 생성 완료 fileName={}, lines={}", fileName, flatRows.size());
            saveReceiptLocal(fileName, content); //영수증테스트
            // 6) SFTP 전송
            logger.info("SFTP 전송 시작 fileName={}", fileName);
            new SftpSender().send(fileName, content);
            logger.info("SFTP 전송 성공 fileName={}", fileName);

            // 7) 2 + 3 둘 다 성공했을 때만 commit
            session.commit();
            logger.info("주문 처리 커밋 완료 firstOrderId={}", firstOrderId);

            return firstOrderId;

        } catch (Exception e) {
            if (session != null) {
                session.rollback();
            }
            logger.error("주문 처리 실패 (rollback) firstOrderId={}", firstOrderId, e);
            throw new RuntimeException("주문 처리 실패", e);

        } finally {
            if (session != null) {
                session.close();
            }
            MDC.remove("orderId");
        }
    }

    /**
     * 파일 내용 생성
     */
    private String buildReceiptContent(List<OrderEntity> rows) {
        StringBuilder sb = new StringBuilder();

        for (OrderEntity r : rows) {
            sb.append(r.getOrderId()).append("^")
                    .append(r.getUserId()).append("^")
                    .append(r.getItemId()).append("^")
                    .append(r.getApplicantKey()).append("^")
                    .append(r.getName()).append("^")
                    .append(r.getAddress()).append("^")
                    .append(r.getItemName()).append("^")
                    .append(r.getPrice())
                    .append("\n");
        }

        return sb.toString();
    }

    private void saveReceiptLocal(String fileName, String content) throws Exception {

        Path dir = Paths.get("receipts"); // 프로젝트 루트에 생성됨(테스트 확인용)

        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        Path file = dir.resolve(fileName);

        Files.writeString(
                file,
                content,
                StandardCharsets.UTF_8 //
        );

        //logger.info("영수증 로컬 저장 완료 path={}", file.toAbsolutePath());
    }
}
