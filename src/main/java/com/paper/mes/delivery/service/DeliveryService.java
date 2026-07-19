package com.paper.mes.delivery.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.paper.mes.common.PageResult;
import com.paper.mes.delivery.dto.AvailableFinishVO;
import com.paper.mes.delivery.dto.DeliveryAppendItemsDTO;
import com.paper.mes.delivery.dto.DeliveryBatchConfirmDTO;
import com.paper.mes.delivery.dto.DeliveryCancelDTO;
import com.paper.mes.delivery.dto.DeliveryConfirmDTO;
import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.delivery.dto.DeliveryDetailVO;
import com.paper.mes.delivery.dto.DeliveryQuery;
import com.paper.mes.delivery.dto.DeliveryRollbackDTO;
import com.paper.mes.delivery.entity.DeliveryOrder;

import java.util.List;

public interface DeliveryService extends IService<DeliveryOrder> {

    PageResult<DeliveryOrder> page(DeliveryQuery query);

    /** 列出某客户全部已入库(2)、可勾选出库的成品（含 source_type=2 直发）。 */
    List<AvailableFinishVO> listAvailable(String customerUuid);
    List<AvailableFinishVO> listAvailable(String customerUuid, String warehouseUuid);

    /**
     * 创建出库单（待出库）：校验成品归属与状态、现结拦截、登记明细。不在此扣库存。
     * 返回出库单 uuid。
     */
    String create(DeliveryCreateDTO dto);

    DeliveryDetailVO getDetail(String uuid);

    /** 导出出库单详情 Excel。 */
    /** 按当前筛选条件导出全部出库单。 */
    /**
     * 出库确认：成品 已入库(2)→已出库(3)，出库单 待出库(1)→已出库签收(2)，写操作日志。
     */
    void confirm(String uuid, DeliveryConfirmDTO dto);

    /** 原子批量签收：任一单据校验失败时全部回滚。 */
    void confirmBatch(DeliveryBatchConfirmDTO dto);

    /** 已出库签收回退：出库单 2→1，明细成品 已出库(3)→已入库(2)。 */
    void rollback(String uuid, DeliveryRollbackDTO dto);

    /** 待出库改单：向单据追加可出库成品，并重算件数与重量。 */
    void appendDetails(String uuid, DeliveryAppendItemsDTO dto);

    /** 待出库改单：从单据中移出一条明细，并重算件数与重量。 */
    void removeDetail(String uuid, String detailUuid);

    /** 作废待出库单并释放全部库存占用。 */
    void cancelPending(String uuid, DeliveryCancelDTO dto);
}
