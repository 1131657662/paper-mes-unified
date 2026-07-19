package com.paper.mes.delivery.mapper;

import com.paper.mes.delivery.dto.DeliveryInventoryCustomerQuery;
import com.paper.mes.delivery.dto.DeliveryInventoryCustomerVO;
import com.paper.mes.delivery.dto.DeliveryInventoryFilter;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishQuery;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishVO;
import com.paper.mes.delivery.dto.DeliveryInventorySummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeliveryInventoryMapper {

    DeliveryInventorySummaryVO summary(@Param("q") DeliveryInventoryFilter filter);

    long customerCount(@Param("q") DeliveryInventoryCustomerQuery query);

    List<DeliveryInventoryCustomerVO> customerRows(@Param("q") DeliveryInventoryCustomerQuery query,
                                                    @Param("offset") long offset,
                                                    @Param("limit") long limit);

    long finishCount(@Param("q") DeliveryInventoryFinishQuery query);

    List<DeliveryInventoryFinishVO> finishRows(@Param("q") DeliveryInventoryFinishQuery query,
                                               @Param("offset") long offset,
                                               @Param("limit") long limit);

    List<DeliveryInventoryFinishVO> availabilityRows(@Param("customerUuid") String customerUuid,
                                                      @Param("warehouseUuid") String warehouseUuid,
                                                      @Param("finishUuids") List<String> finishUuids);
}
