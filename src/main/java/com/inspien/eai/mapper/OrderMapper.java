package com.inspien.eai.mapper;

import com.inspien.eai.domain.OrderEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface OrderMapper {

    String selectMaxOrderId();

    int insertOrder(OrderEntity order);

    List<OrderEntity> selectNotSentOrders(@Param("applicantKey") String applicantKey);
}
