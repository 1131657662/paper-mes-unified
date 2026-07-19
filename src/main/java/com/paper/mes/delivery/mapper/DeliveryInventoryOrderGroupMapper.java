package com.paper.mes.delivery.mapper;

import com.paper.mes.delivery.dto.DeliveryInventoryFinishQuery;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishVO;
import com.paper.mes.delivery.dto.DeliveryInventoryOrderGroupVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeliveryInventoryOrderGroupMapper {

    long count(@Param("q") DeliveryInventoryFinishQuery query);

    List<DeliveryInventoryOrderGroupVO> rows(@Param("q") DeliveryInventoryFinishQuery query,
                                              @Param("offset") long offset,
                                              @Param("limit") long limit);

    List<DeliveryInventoryFinishVO> finishRows(@Param("q") DeliveryInventoryFinishQuery query,
                                                @Param("orderUuids") List<String> orderUuids);
}
