package com.paper.mes.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.PageResult;
import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.delivery.dto.AvailableFinishVO;
import com.paper.mes.delivery.dto.DeliveryConfirmDTO;
import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.delivery.dto.DeliveryDetailVO;
import com.paper.mes.delivery.dto.DeliveryQuery;
import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.delivery.mapper.DeliveryOrderMapper;
import com.paper.mes.delivery.service.DeliveryService;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl extends ServiceImpl<DeliveryOrderMapper, DeliveryOrder>
        implements DeliveryService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String DELIVERY_NO_PREFIX = "CK";

    private static final int FINISH_STATUS_IN_STOCK = 2;
    private static final int FINISH_STATUS_OUT = 3;
    private static final int DELIVERY_STATUS_PENDING = 1;
    private static final int DELIVERY_STATUS_OUT = 2;

    private static final int SETTLE_TYPE_CASH = 1;
    private static final int SETTLE_STATUS_PENDING = 1;
    private static final int SETTLE_STATUS_PARTIAL = 2;

    private static final int BLOCK_NONE = 0;
    private static final int BLOCK_RELEASE = 1;
    private static final int BLOCK_REJECT = 2;

    private final DeliveryDetailMapper deliveryDetailMapper;
    private final FinishRollMapper finishRollMapper;
    private final ProcessOrderMapper processOrderMapper;
    private final SettleOrderMapper settleOrderMapper;
    private final CustomerService customerService;
    private final OperationLogService operationLogService;

    @Override
    public PageResult<DeliveryOrder> page(DeliveryQuery query) {
        LambdaQueryWrapper<DeliveryOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(DeliveryOrder::getDeliveryNo, kw)
                    .or().like(DeliveryOrder::getCustomerName, kw));
        }
        if (StringUtils.hasText(query.getCustomerUuid())) {
            wrapper.eq(DeliveryOrder::getCustomerUuid, query.getCustomerUuid());
        }
        if (query.getDeliveryStatus() != null) {
            wrapper.eq(DeliveryOrder::getDeliveryStatus, query.getDeliveryStatus());
        }
        wrapper.orderByDesc(DeliveryOrder::getCreateTime);
        Page<DeliveryOrder> page = page(Page.of(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.of(page);
    }

    @Override
    public List<AvailableFinishVO> listAvailable(String customerUuid) {
        if (!StringUtils.hasText(customerUuid)) {
            throw new BusinessException("客户不能为空");
        }
        // 该客户全部加工单 → uuid→单号映射。
        List<ProcessOrder> orders = processOrderMapper.selectList(
                new LambdaQueryWrapper<ProcessOrder>()
                        .eq(ProcessOrder::getCustomerUuid, customerUuid));
        if (orders.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, String> orderNoByUuid = new LinkedHashMap<>();
        Map<String, ProcessOrder> orderByUuid = new LinkedHashMap<>();
        for (ProcessOrder o : orders) {
            orderNoByUuid.put(o.getUuid(), o.getOrderNo());
            orderByUuid.put(o.getUuid(), o);
        }
        List<String> lockedFinishUuids = pendingDeliveryFinishUuids();
        boolean settlementRisk = hasUnsettledCash(customerUuid);
        LambdaQueryWrapper<FinishRoll> finishWrapper = new LambdaQueryWrapper<FinishRoll>()
                .in(FinishRoll::getOrderUuid, orderNoByUuid.keySet())
                .eq(FinishRoll::getFinishStatus, FINISH_STATUS_IN_STOCK)
                .orderByAsc(FinishRoll::getOrderUuid)
                .orderByAsc(FinishRoll::getRowSort);
        if (!lockedFinishUuids.isEmpty()) {
            finishWrapper.notIn(FinishRoll::getUuid, lockedFinishUuids);
        }
        List<FinishRoll> finishes = finishRollMapper.selectList(
                finishWrapper);
        List<AvailableFinishVO> list = new ArrayList<>(finishes.size());
        for (FinishRoll f : finishes) {
            AvailableFinishVO vo = new AvailableFinishVO();
            vo.setFinishUuid(f.getUuid());
            vo.setFinishRollNo(f.getFinishRollNo());
            vo.setOrderUuid(f.getOrderUuid());
            vo.setOrderNo(orderNoByUuid.get(f.getOrderUuid()));
            ProcessOrder order = orderByUuid.get(f.getOrderUuid());
            if (order != null) {
                vo.setOrderDate(order.getOrderDate());
                vo.setSettleType(order.getSettleType());
                vo.setSettleDay(order.getSettleDay());
                vo.setIsInvoice(order.getIsInvoice());
            }
            vo.setPaperName(f.getPaperName());
            vo.setGramWeight(f.getGramWeight());
            vo.setFinishWidth(f.getFinishWidth());
            vo.setFinishDiameter(f.getFinishDiameter());
            vo.setFinishCoreDiameter(f.getFinishCoreDiameter());
            vo.setActualWeight(f.getActualWeight());
            vo.setSourceType(f.getSourceType());
            vo.setFinishStatus(f.getFinishStatus());
            vo.setSettlementRisk(settlementRisk && vo.getSettleType() != null
                    && vo.getSettleType() == SETTLE_TYPE_CASH);
            list.add(vo);
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(DeliveryCreateDTO dto) {
        Customer customer = customerService.getById(dto.getCustomerUuid());
        if (customer == null) {
            throw new BusinessException(ErrorCode.E002, "客户不存在");
        }

        // 逐件校验成品：存在、已入库(2)、归属当前客户。
        List<FinishRoll> picked = new ArrayList<>(dto.getItems().size());
        Map<String, String> orderNoCache = new LinkedHashMap<>();
        List<String> lockedFinishUuids = pendingDeliveryFinishUuids();
        boolean hasCashOrder = false;
        for (DeliveryCreateDTO.Item item : dto.getItems()) {
            if (lockedFinishUuids.contains(item.getFinishUuid())) {
                throw new BusinessException("成品已在待出库单中，不能重复创建出库：" + item.getFinishUuid());
            }
            FinishRoll f = finishRollMapper.selectById(item.getFinishUuid());
            if (f == null) {
                throw new BusinessException(ErrorCode.E002, "成品不存在：" + item.getFinishUuid());
            }
            if (f.getFinishStatus() == null || f.getFinishStatus() != FINISH_STATUS_IN_STOCK) {
                throw new BusinessException("成品非已入库状态，不可出库：" + f.getFinishRollNo());
            }
            ProcessOrder order = processOrderMapper.selectById(f.getOrderUuid());
            if (order == null || !dto.getCustomerUuid().equals(order.getCustomerUuid())) {
                throw new BusinessException("成品不属于该客户：" + f.getFinishRollNo());
            }
            if (order.getSettleType() != null && order.getSettleType() == SETTLE_TYPE_CASH) {
                hasCashOrder = true;
            }
            orderNoCache.put(f.getOrderUuid(), order.getOrderNo());
            picked.add(f);
        }

        // 现结拦截：以加工单结算快照为准，避免客户档案后续调整影响历史单据。
        int blockAction = BLOCK_NONE;
        if (hasCashOrder && hasUnsettledCash(dto.getCustomerUuid())) {
            if (!dto.isForceRelease()) {
                throw new BusinessException("次结加工单存在未结清款项，禁止出库");
            }
            blockAction = BLOCK_RELEASE;
        }

        LocalDate date = dto.getDeliveryDate();
        DeliveryOrder deliveryOrder = new DeliveryOrder();
        deliveryOrder.setDeliveryNo(nextDeliveryNo(date));
        deliveryOrder.setCustomerUuid(dto.getCustomerUuid());
        deliveryOrder.setCustomerName(customer.getCustomerName());
        deliveryOrder.setDeliveryDate(date);
        deliveryOrder.setPickerName(dto.getPickerName());
        deliveryOrder.setCarNo(dto.getCarNo());
        deliveryOrder.setContainerNo(dto.getContainerNo());
        deliveryOrder.setRemark(dto.getRemark());
        deliveryOrder.setSettleBlockAction(blockAction);
        deliveryOrder.setDeliveryStatus(DELIVERY_STATUS_PENDING);

        BigDecimal totalWeight = BigDecimal.ZERO;
        List<DeliveryDetail> details = new ArrayList<>(picked.size());
        for (int i = 0; i < picked.size(); i++) {
            FinishRoll f = picked.get(i);
            DeliveryCreateDTO.Item item = dto.getItems().get(i);
            BigDecimal outWeight = item.getOutWeight() != null ? item.getOutWeight()
                    : (f.getActualWeight() != null ? f.getActualWeight() : BigDecimal.ZERO);
            totalWeight = totalWeight.add(outWeight);

            DeliveryDetail d = new DeliveryDetail();
            d.setFinishUuid(f.getUuid());
            d.setOrderUuid(f.getOrderUuid());
            d.setFinishRollNo(f.getFinishRollNo());
            d.setPaperName(f.getPaperName());
            d.setOutWeight(outWeight);
            d.setRemark(item.getRemark());
            details.add(d);
        }
        deliveryOrder.setTotalCount(picked.size());
        deliveryOrder.setTotalWeight(totalWeight);
        save(deliveryOrder);

        for (DeliveryDetail d : details) {
            d.setDeliveryUuid(deliveryOrder.getUuid());
            deliveryDetailMapper.insert(d);
        }

        if (blockAction == BLOCK_RELEASE) {
            operationLogService.record(OperationLogService.BIZ_TYPE_DELIVERY,
                    deliveryOrder.getUuid(), deliveryOrder.getDeliveryNo(),
                    OperationLogService.ACTION_DELIVERY_RELEASE, null,
                    "次结客户未结清，授权放行出库");
        }
        return deliveryOrder.getUuid();
    }

    @Override
    public DeliveryDetailVO getDetail(String uuid) {
        DeliveryOrder order = getById(uuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "出库单不存在");
        }
        List<DeliveryDetail> details = deliveryDetailMapper.selectList(
                new LambdaQueryWrapper<DeliveryDetail>()
                        .eq(DeliveryDetail::getDeliveryUuid, uuid));
        DeliveryDetailVO vo = new DeliveryDetailVO();
        vo.setOrder(order);
        vo.setDetails(details);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(String uuid, DeliveryConfirmDTO dto) {
        DeliveryOrder order = getById(uuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "出库单不存在");
        }
        if (order.getDeliveryStatus() == null || order.getDeliveryStatus() != DELIVERY_STATUS_PENDING) {
            throw new BusinessException("出库单非待出库状态，不可确认");
        }
        List<DeliveryDetail> details = deliveryDetailMapper.selectList(
                new LambdaQueryWrapper<DeliveryDetail>()
                        .eq(DeliveryDetail::getDeliveryUuid, uuid));
        // 逐件扣库存：成品须仍为已入库(2)，置为已出库(3)。
        for (DeliveryDetail d : details) {
            FinishRoll f = finishRollMapper.selectById(d.getFinishUuid());
            if (f == null || f.getFinishStatus() == null
                    || f.getFinishStatus() != FINISH_STATUS_IN_STOCK) {
                throw new BusinessException("成品状态已变更，不可出库：" + d.getFinishRollNo());
            }
            f.setFinishStatus(FINISH_STATUS_OUT);
            finishRollMapper.updateById(f);
        }

        order.setDeliveryStatus(DELIVERY_STATUS_OUT);
        order.setSignUser(dto == null ? null : dto.getSignUser());
        order.setSignTime(dto != null && dto.getSignTime() != null ? dto.getSignTime() : LocalDateTime.now());
        if (dto != null && StringUtils.hasText(dto.getRemark())) {
            order.setRemark(dto.getRemark());
        }
        updateById(order);

        operationLogService.record(OperationLogService.BIZ_TYPE_DELIVERY,
                order.getUuid(), order.getDeliveryNo(),
                OperationLogService.ACTION_DELIVERY_CONFIRM,
                dto == null ? null : dto.getSignUser(), "出库确认签收");
    }

    /** 出库单号：CK + yyyyMMdd + 当天日内 4 位流水。唯一索引兜底防并发重复。 */
    private String nextDeliveryNo(LocalDate date) {
        String prefix = DELIVERY_NO_PREFIX + date.format(DAY_FMT);
        long todayCount = count(new LambdaQueryWrapper<DeliveryOrder>()
                .likeRight(DeliveryOrder::getDeliveryNo, prefix));
        return prefix + String.format("%04d", todayCount + 1);
    }

    private boolean hasUnsettledCash(String customerUuid) {
        long unsettled = settleOrderMapper.selectCount(
                new LambdaQueryWrapper<SettleOrder>()
                        .eq(SettleOrder::getCustomerUuid, customerUuid)
                        .in(SettleOrder::getSettleStatus, SETTLE_STATUS_PENDING, SETTLE_STATUS_PARTIAL));
        return unsettled > 0;
    }

    private List<String> pendingDeliveryFinishUuids() {
        List<DeliveryOrder> pendingOrders = list(new LambdaQueryWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_PENDING));
        if (pendingOrders.isEmpty()) {
            return List.of();
        }
        List<String> deliveryUuids = pendingOrders.stream().map(DeliveryOrder::getUuid).toList();
        List<DeliveryDetail> details = deliveryDetailMapper.selectList(
                new LambdaQueryWrapper<DeliveryDetail>()
                        .in(DeliveryDetail::getDeliveryUuid, deliveryUuids));
        return details.stream().map(DeliveryDetail::getFinishUuid).toList();
    }
}
