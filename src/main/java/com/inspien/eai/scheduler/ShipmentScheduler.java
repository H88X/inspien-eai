package com.inspien.eai.scheduler;

import com.inspien.eai.service.ShipmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 5분 주기 배치 스케줄러
 */
public class ShipmentScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentScheduler.class);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ShipmentService shipmentService = new ShipmentService();

    public void start() {

        //logger.info("ShipmentScheduler 시작 (5분 주기)");

        scheduler.scheduleAtFixedRate(() -> {
            String traceId = "BATCH-" + UUID.randomUUID().toString().substring(0, 8);
            MDC.put("traceId", traceId);

            try {
                logger.info("ShipmentScheduler 배치 시작");
                shipmentService.process();
                logger.info("ShipmentScheduler 배치 성공");
            } catch (Exception e) {
                logger.error("ShipmentScheduler 배치 실패", e);
            } finally {
                MDC.remove("traceId");
            }

        }, 0, 5, TimeUnit.MINUTES);
    }
}
