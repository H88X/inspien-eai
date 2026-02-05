package com.inspien.eai.mapper;

import com.inspien.eai.domain.ShipmentEntity;
import org.apache.ibatis.annotations.Param;

public interface ShipmentMapper {

    String selectMaxShipmentId(@Param("applicantKey") String applicantKey);

    int insertShipment(ShipmentEntity shipment);

    int updateOrderStatus(@Param("orderId") String orderId,
                          @Param("applicantKey") String applicantKey);
}
