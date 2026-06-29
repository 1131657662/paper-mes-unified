package com.paper.mes.delivery.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.paper.mes.common.PageResult;
import com.paper.mes.delivery.dto.AvailableFinishVO;
import com.paper.mes.delivery.dto.DeliveryConfirmDTO;
import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.delivery.dto.DeliveryDetailVO;
import com.paper.mes.delivery.dto.DeliveryQuery;
import com.paper.mes.delivery.entity.DeliveryOrder;

import java.util.List;

public interface DeliveryService extends IService<DeliveryOrder> {

    PageResult<DeliveryOrder> page(DeliveryQuery query);

    /** 列出某客户全部已入库(2)、可勾选出库的成品（含 source_type=2 直发）。 */
    List<AvailableFinishVO> listAvailable(String customerUuid);

    /**
     * 创建出库单（待出库）：校验成品归属与状态、现结拦截、登记明细。不在此扣库存。
     * 返回出库单 uuid。
     */
    String create(DeliveryCreateDTO dto);

    DeliveryDetailVO getDetail(String uuid);

    /**
     * 出库确认：成品 已入库(2)→已出库(3)，出库单 待出库(1)→已出库签收(2)，写操作日志。
     */
    void confirm(String uuid, DeliveryConfirmDTO dto);
}
