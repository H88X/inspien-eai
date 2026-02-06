package com.inspien.eai;

import com.inspien.eai.controller.OrderController;
import com.inspien.eai.scheduler.ShipmentScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);


    public static void main(String[] args) {

        try {
            // REST API 서버 시작
            OrderController.startServer();

            // 배치 스케줄러 시작
            new ShipmentScheduler().start();

            logger.info("Application Started");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
