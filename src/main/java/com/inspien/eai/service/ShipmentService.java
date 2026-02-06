package com.inspien.eai.service;

import com.inspien.eai.common.Constants;
import com.inspien.eai.config.MyBatisConfig;
import com.inspien.eai.domain.OrderEntity;
import com.inspien.eai.domain.ShipmentEntity;
import com.inspien.eai.mapper.OrderMapper;
import com.inspien.eai.mapper.ShipmentMapper;
import com.inspien.eai.util.ShipmentIDGenerator;
import org.apache.ibatis.session.SqlSession;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ShipmentService {

    public void process() {

        try (SqlSession session = MyBatisConfig.getFactory().openSession(false)) {

            OrderMapper orderMapper = session.getMapper(OrderMapper.class);
            ShipmentMapper shipmentMapper = session.getMapper(ShipmentMapper.class);

            // 미전송 주문 조회 (조건: applicantKey + status=N)
            List<OrderEntity> orders = orderMapper.selectNotSentOrders(Constants.APPLICANT_KEY);

            if (orders == null || orders.isEmpty()) {
                session.commit();
                return;
            }

            // 현재 최대 SHIPMENT_ID 조회 (K로 시작)
            String maxShipmentId = shipmentMapper.selectMaxShipmentId(Constants.APPLICANT_KEY);

            Set<String> updatedOrderIds = new HashSet<>();

            for (OrderEntity order : orders) {

                // 다음 SHIPMENT_ID 생성
                maxShipmentId = ShipmentIDGenerator.generate(maxShipmentId);

                ShipmentEntity shipment = new ShipmentEntity();
                shipment.setShipmentId(maxShipmentId);
                shipment.setOrderId(order.getOrderId());
                shipment.setItemId(order.getItemId());
                shipment.setApplicantKey(Constants.APPLICANT_KEY);
                shipment.setAddress(order.getAddress());

                int inserted = shipmentMapper.insertShipment(shipment);

                // insert 성공한 건에 한해서만 status 업데이트
                if (inserted == 1) {
                    if (updatedOrderIds.add(order.getOrderId())) {
                        shipmentMapper.updateOrderStatus(order.getOrderId(), Constants.APPLICANT_KEY);
                    }
                } else {
                    // insert가 실패하면 롤백
                    throw new IllegalStateException("SHIPMENT_TB insert 실패: orderId=" + order.getOrderId());
                }
            }

            session.commit();

        } catch (Exception e) {
            throw new RuntimeException("Shipment 배치 처리 실패", e);
        }
    }
}
