package com.paper.mes.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.paper.mes.delivery.entity.DeliveryDetail;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeliveryDetailMapper extends BaseMapper<DeliveryDetail> {

    long countBlockingDeliveryActivity(@Param("finishUuids") List<String> finishUuids);
}
