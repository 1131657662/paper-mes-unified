package com.paper.mes.delivery.mapper;

import com.paper.mes.delivery.dto.DeliveryInventoryUnassignedOrderVO;
import com.paper.mes.delivery.dto.DeliveryInventoryUnassignedQuery;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeliveryInventoryWarehouseRepairMapper {

    long countUnassignedOrders(@Param("q") DeliveryInventoryUnassignedQuery query);

    List<DeliveryInventoryUnassignedOrderVO> selectUnassignedOrders(
            @Param("q") DeliveryInventoryUnassignedQuery query,
            @Param("offset") long offset,
            @Param("limit") long limit);

    List<ProcessOrder> selectOrdersForRepair(@Param("orderUuids") List<String> orderUuids);

    List<FinishRoll> selectFinishesForRepair(@Param("orderUuids") List<String> orderUuids);

    long countActiveLocks(@Param("finishUuids") List<String> finishUuids);

    int assignFinishWarehouse(@Param("finishUuids") List<String> finishUuids,
                              @Param("warehouseUuid") String warehouseUuid,
                              @Param("operator") String operator);

    int assignOrderWarehouse(@Param("orderUuid") String orderUuid,
                             @Param("warehouseUuid") String warehouseUuid,
                             @Param("snapFinish") String snapFinish,
                             @Param("operator") String operator);
}
