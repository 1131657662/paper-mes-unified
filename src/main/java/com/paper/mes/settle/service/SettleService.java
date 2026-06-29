package com.paper.mes.settle.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.paper.mes.common.PageResult;
import com.paper.mes.settle.dto.ReceiveDTO;
import com.paper.mes.settle.dto.SettleByMonthDTO;
import com.paper.mes.settle.dto.SettleByOrderDTO;
import com.paper.mes.settle.dto.SettleByOrdersDTO;
import com.paper.mes.settle.dto.SettleCandidateQuery;
import com.paper.mes.settle.dto.SettleCandidateVO;
import com.paper.mes.settle.dto.SettleDetailVO;
import com.paper.mes.settle.dto.SettleQuery;
import com.paper.mes.settle.entity.SettleOrder;

import java.util.List;

public interface SettleService extends IService<SettleOrder> {

    PageResult<SettleOrder> page(SettleQuery query);

    /** 列出已完成且未结算的加工单，作为结算工作台的候选范围。 */
    List<SettleCandidateVO> listCandidates(SettleCandidateQuery query);

    /**
     * 按单生成结算单：校验加工单 已完成(4)，分项汇总锯纸/复卷费，装配结算单，
     * 加工单 已完成(4)→已结算(5)。返回结算单 uuid。
     */
    String createByOrder(SettleByOrderDTO dto);

    /** 勾选一张或多张加工单生成结算单。 */
    String createByOrders(SettleByOrdersDTO dto);

    /**
     * 按月批量生成结算单：圈出客户账期内 已完成(4) 全部加工单，逐单累加，
     * 各单 已完成(4)→已结算(5)。返回结算单 uuid。
     */
    String createByMonth(SettleByMonthDTO dto);

    SettleDetailVO getDetail(String uuid);

    /**
     * 登记一笔收款：校验未结清、不超收，更新已收/未收/结算状态，写收款流水与操作日志。
     */
    void receive(String uuid, ReceiveDTO dto);
}
