package com.paper.mes.settle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.PageResult;
import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.settle.dto.ReceiveDTO;
import com.paper.mes.settle.dto.SettleByMonthDTO;
import com.paper.mes.settle.dto.SettleByOrderDTO;
import com.paper.mes.settle.dto.SettleByOrdersDTO;
import com.paper.mes.settle.dto.SettleCandidateQuery;
import com.paper.mes.settle.dto.SettleCandidateVO;
import com.paper.mes.settle.dto.SettleDetailVO;
import com.paper.mes.settle.dto.SettleQuery;
import com.paper.mes.settle.entity.ReceiveRecord;
import com.paper.mes.settle.entity.SettleDetail;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.ReceiveRecordMapper;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import com.paper.mes.settle.service.SettleCandidateStatsLoader;
import com.paper.mes.settle.service.SettleService;
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
public class SettleServiceImpl extends ServiceImpl<SettleOrderMapper, SettleOrder>
        implements SettleService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String SETTLE_NO_PREFIX = "JS";

    private static final int ORDER_STATUS_FINISHED = 4;
    private static final int ORDER_STATUS_SETTLED = 5;

    private static final int SETTLE_TYPE_BY_ORDER = 1;
    private static final int SETTLE_TYPE_BY_MONTH = 2;

    private static final int SETTLE_STATUS_PENDING = 1;
    private static final int SETTLE_STATUS_PARTIAL = 2;
    private static final int SETTLE_STATUS_CLEARED = 3;

    private static final int STEP_TYPE_SAW = 1;
    private static final int STEP_TYPE_REWIND = 2;

    private final SettleDetailMapper settleDetailMapper;
    private final ReceiveRecordMapper receiveRecordMapper;
    private final ProcessStepMapper processStepMapper;
    private final ProcessOrderService processOrderService;
    private final CustomerService customerService;
    private final OperationLogService operationLogService;
    private final SettleCandidateStatsLoader statsLoader;

    @Override
    public PageResult<SettleOrder> page(SettleQuery query) {
        LambdaQueryWrapper<SettleOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(SettleOrder::getSettleNo, kw)
                    .or().like(SettleOrder::getCustomerName, kw));
        }
        if (StringUtils.hasText(query.getCustomerUuid())) {
            wrapper.eq(SettleOrder::getCustomerUuid, query.getCustomerUuid());
        }
        if (query.getSettleStatus() != null) {
            wrapper.eq(SettleOrder::getSettleStatus, query.getSettleStatus());
        }
        if (query.getSettleType() != null) {
            wrapper.eq(SettleOrder::getSettleType, query.getSettleType());
        }
        if (query.getDateFrom() != null) {
            wrapper.ge(SettleOrder::getSettleDate, query.getDateFrom());
        }
        if (query.getDateTo() != null) {
            wrapper.le(SettleOrder::getSettleDate, query.getDateTo());
        }
        wrapper.orderByDesc(SettleOrder::getCreateTime);
        Page<SettleOrder> page = page(Page.of(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.of(page);
    }

    @Override
    public List<SettleCandidateVO> listCandidates(SettleCandidateQuery query) {
        LambdaQueryWrapper<ProcessOrder> wrapper = new LambdaQueryWrapper<ProcessOrder>()
                .eq(ProcessOrder::getOrderStatus, ORDER_STATUS_FINISHED);
        if (StringUtils.hasText(query.getCustomerUuid())) {
            wrapper.eq(ProcessOrder::getCustomerUuid, query.getCustomerUuid());
        }
        if (query.getPeriodStart() != null) {
            wrapper.ge(ProcessOrder::getOrderDate, query.getPeriodStart());
        }
        if (query.getPeriodEnd() != null) {
            wrapper.le(ProcessOrder::getOrderDate, query.getPeriodEnd());
        }
        wrapper.orderByAsc(ProcessOrder::getOrderDate).orderByAsc(ProcessOrder::getOrderNo);
        List<ProcessOrder> orders = processOrderService.list(wrapper);
        Map<String, SettleCandidateStatsLoader.CandidateStats> statsByOrder =
                statsLoader.load(orders.stream().map(ProcessOrder::getUuid).toList());
        List<SettleCandidateVO> result = new ArrayList<>(orders.size());
        for (ProcessOrder order : orders) {
            SettleDetail amount = buildDetail(order);
            SettleCandidateStatsLoader.CandidateStats stats = statsByOrder.get(order.getUuid());
            SettleCandidateVO vo = new SettleCandidateVO();
            vo.setOrderUuid(order.getUuid());
            vo.setOrderNo(order.getOrderNo());
            vo.setCustomerUuid(order.getCustomerUuid());
            vo.setCustomerName(order.getCustomerName());
            vo.setOrderDate(order.getOrderDate());
            vo.setSettleType(order.getSettleType() == null ? SETTLE_TYPE_BY_MONTH : order.getSettleType());
            vo.setSettleDay(order.getSettleDay());
            vo.setIsInvoice(order.getIsInvoice() == null ? 2 : order.getIsInvoice());
            vo.setOriginalRollCount(stats == null ? 0 : stats.originalRollCount());
            vo.setOriginalRollWeight(stats == null ? BigDecimal.ZERO : stats.originalRollWeight());
            vo.setFinishRollCount(stats == null ? 0 : stats.finishRollCount());
            vo.setFinishRollWeight(stats == null ? BigDecimal.ZERO : stats.finishRollWeight());
            vo.setSawAmount(amount.getSawAmount());
            vo.setRewindAmount(amount.getRewindAmount());
            vo.setExtraAmount(amount.getExtraAmount());
            vo.setTotalAmount(amount.getOrderAmount());
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createByOrder(SettleByOrderDTO dto) {
        ProcessOrder order = processOrderService.getById(dto.getOrderUuid());
        validateFinishedOrder(order);
        String settleUuid = createFromOrders(new SettlementBuildContext(
                List.of(order),
                dto.getSettleDate() != null ? dto.getSettleDate() : LocalDate.now(),
                SETTLE_TYPE_BY_ORDER,
                null,
                null,
                dto.getIsInvoice(),
                dto.getRemark()));
        SettleOrder settle = getById(settleUuid);
        operationLogService.record(OperationLogService.BIZ_TYPE_SETTLE,
                settle.getUuid(), settle.getSettleNo(),
                OperationLogService.ACTION_SETTLE, null,
                "按单生成结算单，加工单：" + order.getOrderNo());
        return settleUuid;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createByOrders(SettleByOrdersDTO dto) {
        List<ProcessOrder> orders = loadOrdersByUuid(dto.getOrderUuids());
        String settleUuid = createFromOrders(new SettlementBuildContext(
                orders,
                dto.getSettleDate() != null ? dto.getSettleDate() : LocalDate.now(),
                orders.size() == 1 ? SETTLE_TYPE_BY_ORDER : SETTLE_TYPE_BY_MONTH,
                dto.getPeriodStart(),
                dto.getPeriodEnd(),
                dto.getIsInvoice(),
                dto.getRemark()));
        SettleOrder settle = getById(settleUuid);
        operationLogService.record(OperationLogService.BIZ_TYPE_SETTLE,
                settle.getUuid(), settle.getSettleNo(),
                OperationLogService.ACTION_SETTLE, null,
                "勾选生成结算单，含加工单 " + orders.size() + " 张");
        return settleUuid;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createByMonth(SettleByMonthDTO dto) {
        Customer customer = customerService.getById(dto.getCustomerUuid());
        if (customer == null) {
            throw new BusinessException(ErrorCode.E002, "客户不存在");
        }
        LocalDateTime from = dto.getPeriodStart().atStartOfDay();
        LocalDateTime to = dto.getPeriodEnd().atTime(23, 59, 59);
        List<ProcessOrder> orders = processOrderService.list(
                new LambdaQueryWrapper<ProcessOrder>()
                        .eq(ProcessOrder::getCustomerUuid, dto.getCustomerUuid())
                        .eq(ProcessOrder::getOrderStatus, ORDER_STATUS_FINISHED)
                        .ge(ProcessOrder::getCreateTime, from)
                        .le(ProcessOrder::getCreateTime, to)
                        .orderByAsc(ProcessOrder::getCreateTime));
        if (orders.isEmpty()) {
            throw new BusinessException("该期间无可结算加工单");
        }

        String settleUuid = createFromOrders(new SettlementBuildContext(
                orders,
                dto.getSettleDate() != null ? dto.getSettleDate() : LocalDate.now(),
                SETTLE_TYPE_BY_MONTH,
                dto.getPeriodStart(),
                dto.getPeriodEnd(),
                dto.getIsInvoice() != null ? dto.getIsInvoice() : customer.getDefaultInvoice(),
                dto.getRemark()));
        SettleOrder settle = getById(settleUuid);
        operationLogService.record(OperationLogService.BIZ_TYPE_SETTLE,
                settle.getUuid(), settle.getSettleNo(),
                OperationLogService.ACTION_SETTLE, null,
                "按月生成结算单，含加工单 " + orders.size() + " 张");
        return settleUuid;
    }

    @Override
    public SettleDetailVO getDetail(String uuid) {
        SettleOrder order = getById(uuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "结算单不存在");
        }
        List<SettleDetail> details = settleDetailMapper.selectList(
                new LambdaQueryWrapper<SettleDetail>()
                        .eq(SettleDetail::getSettleUuid, uuid));
        List<ReceiveRecord> receives = receiveRecordMapper.selectList(
                new LambdaQueryWrapper<ReceiveRecord>()
                        .eq(ReceiveRecord::getSettleUuid, uuid)
                        .orderByAsc(ReceiveRecord::getReceiveDate));
        SettleDetailVO vo = new SettleDetailVO();
        vo.setOrder(order);
        vo.setDetails(details);
        vo.setReceives(receives);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(String uuid, ReceiveDTO dto) {
        SettleOrder settle = getById(uuid);
        if (settle == null) {
            throw new BusinessException(ErrorCode.E002, "结算单不存在");
        }
        if (settle.getSettleStatus() != null && settle.getSettleStatus() == SETTLE_STATUS_CLEARED) {
            throw new BusinessException("结算单已结清，不可再收款");
        }
        BigDecimal amount = dto.getReceiveAmount();
        BigDecimal unreceived = nz(settle.getUnreceivedAmount());
        if (amount.compareTo(unreceived) > 0) {
            throw new BusinessException("收款金额超过未收金额，禁止超收");
        }

        ReceiveRecord record = new ReceiveRecord();
        record.setSettleUuid(uuid);
        record.setReceiveDate(dto.getReceiveDate() != null ? dto.getReceiveDate() : LocalDateTime.now());
        record.setReceiveAmount(amount);
        record.setPayMethod(dto.getPayMethod());
        record.setPayNo(dto.getPayNo());
        String operator = resolveOperator(dto.getOperator());
        record.setOperator(operator);
        record.setRemark(dto.getRemark());
        receiveRecordMapper.insert(record);

        BigDecimal received = nz(settle.getReceivedAmount()).add(amount);
        BigDecimal total = nz(settle.getTotalAmount());
        BigDecimal newUnreceived = total.subtract(received);
        settle.setReceivedAmount(received);
        settle.setUnreceivedAmount(newUnreceived);
        if (received.compareTo(BigDecimal.ZERO) <= 0) {
            settle.setSettleStatus(SETTLE_STATUS_PENDING);
        } else if (received.compareTo(total) >= 0) {
            settle.setSettleStatus(SETTLE_STATUS_CLEARED);
        } else {
            settle.setSettleStatus(SETTLE_STATUS_PARTIAL);
        }
        updateById(settle);

        operationLogService.record(OperationLogService.BIZ_TYPE_SETTLE,
                settle.getUuid(), settle.getSettleNo(),
                OperationLogService.ACTION_RECEIVE, operator,
                "登记收款 " + amount + " 元");
    }

    /** 单张加工单装配明细：锯纸/复卷费按 step_type 汇总，extra/order_amount 取加工单落库字段。 */
    private SettleDetail buildDetail(ProcessOrder order) {
        List<ProcessStep> steps = processStepMapper.selectList(
                new LambdaQueryWrapper<ProcessStep>()
                        .eq(ProcessStep::getOrderUuid, order.getUuid()));
        BigDecimal saw = BigDecimal.ZERO;
        BigDecimal rewind = BigDecimal.ZERO;
        for (ProcessStep s : steps) {
            BigDecimal amt = nz(s.getStepAmount());
            if (s.getStepType() != null && s.getStepType() == STEP_TYPE_SAW) {
                saw = saw.add(amt);
            } else if (s.getStepType() != null && s.getStepType() == STEP_TYPE_REWIND) {
                rewind = rewind.add(amt);
            }
        }
        SettleDetail d = new SettleDetail();
        d.setOrderUuid(order.getUuid());
        d.setOrderNo(order.getOrderNo());
        d.setSawAmount(saw);
        d.setRewindAmount(rewind);
        d.setExtraAmount(nz(order.getTotalExtraAmount()));
        d.setOrderAmount(nz(order.getTotalAmount()));
        return d;
    }

    private String createFromOrders(SettlementBuildContext context) {
        if (context.orders().isEmpty()) {
            throw new BusinessException("加工单不能为空");
        }
        Customer customer = resolveSingleCustomer(context.orders());
        SettleOrder settle = new SettleOrder();
        settle.setSettleNo(nextSettleNo(context.settleDate()));
        settle.setCustomerUuid(customer.getUuid());
        settle.setCustomerName(customer.getCustomerName());
        settle.setSettleType(context.settleType());
        settle.setSettleDate(context.settleDate());
        settle.setPeriodStart(context.periodStart());
        settle.setPeriodEnd(context.periodEnd());
        settle.setIsInvoice(resolveInvoice(context, customer));
        settle.setSettleStatus(SETTLE_STATUS_PENDING);
        settle.setReceivedAmount(BigDecimal.ZERO);
        settle.setRemark(context.remark());

        SettlementAmounts amounts = sumAmounts(context.orders());
        settle.setSawAmount(amounts.saw());
        settle.setRewindAmount(amounts.rewind());
        settle.setExtraAmount(amounts.extra());
        settle.setAmountNoTax(amounts.noTax());
        settle.setTaxAmount(amounts.tax());
        settle.setTotalAmount(amounts.total());
        settle.setUnreceivedAmount(amounts.total());
        save(settle);

        for (SettleDetail detail : amounts.details()) {
            detail.setSettleUuid(settle.getUuid());
            settleDetailMapper.insert(detail);
            processOrderService.changeStatus(detail.getOrderUuid(), ORDER_STATUS_SETTLED);
        }
        return settle.getUuid();
    }

    private List<ProcessOrder> loadOrdersByUuid(List<String> orderUuids) {
        List<ProcessOrder> loaded = processOrderService.listByIds(orderUuids);
        if (loaded.size() != orderUuids.size()) {
            throw new BusinessException(ErrorCode.E002, "存在不存在的加工单");
        }
        Map<String, ProcessOrder> byUuid = new LinkedHashMap<>();
        for (ProcessOrder order : loaded) {
            validateFinishedOrder(order);
            byUuid.put(order.getUuid(), order);
        }
        List<ProcessOrder> ordered = new ArrayList<>(orderUuids.size());
        for (String uuid : orderUuids) {
            ordered.add(byUuid.get(uuid));
        }
        return ordered;
    }

    private Customer resolveSingleCustomer(List<ProcessOrder> orders) {
        String customerUuid = orders.get(0).getCustomerUuid();
        for (ProcessOrder order : orders) {
            validateFinishedOrder(order);
            if (!customerUuid.equals(order.getCustomerUuid())) {
                throw new BusinessException("不能跨客户合并生成结算单");
            }
        }
        Customer customer = customerService.getById(customerUuid);
        if (customer == null) {
            throw new BusinessException(ErrorCode.E002, "客户不存在");
        }
        return customer;
    }

    private Integer resolveInvoice(SettlementBuildContext context, Customer customer) {
        if (context.isInvoice() != null) {
            return context.isInvoice();
        }
        Integer orderInvoice = context.orders().get(0).getIsInvoice();
        return orderInvoice != null ? orderInvoice : customer.getDefaultInvoice();
    }

    private SettlementAmounts sumAmounts(List<ProcessOrder> orders) {
        BigDecimal saw = BigDecimal.ZERO;
        BigDecimal rewind = BigDecimal.ZERO;
        BigDecimal extra = BigDecimal.ZERO;
        BigDecimal noTax = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        List<SettleDetail> details = new ArrayList<>(orders.size());
        for (ProcessOrder order : orders) {
            SettleDetail detail = buildDetail(order);
            details.add(detail);
            saw = saw.add(detail.getSawAmount());
            rewind = rewind.add(detail.getRewindAmount());
            extra = extra.add(detail.getExtraAmount());
            noTax = noTax.add(nz(order.getTotalAmountNoTax()));
            tax = tax.add(nz(order.getTotalAmountTax()));
            total = total.add(nz(order.getTotalAmount()));
        }
        return new SettlementAmounts(saw, rewind, extra, noTax, tax, total, details);
    }

    private void validateFinishedOrder(ProcessOrder order) {
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        if (order.getOrderStatus() == null || order.getOrderStatus() != ORDER_STATUS_FINISHED) {
            throw new BusinessException("加工单非已完成状态，不可结算：" + order.getOrderNo());
        }
    }

    /** 结算单号：JS + yyyyMMdd + 当天日内 4 位流水。唯一索引兜底防并发重复。 */
    private String nextSettleNo(LocalDate date) {
        String prefix = SETTLE_NO_PREFIX + date.format(DAY_FMT);
        long todayCount = count(new LambdaQueryWrapper<SettleOrder>()
                .likeRight(SettleOrder::getSettleNo, prefix));
        return prefix + String.format("%04d", todayCount + 1);
    }

    private String resolveOperator(String operator) {
        if (operator != null && !operator.isBlank()) {
            return operator;
        }
        return AuthContextHolder.currentDisplayName();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private record SettlementBuildContext(
            List<ProcessOrder> orders,
            LocalDate settleDate,
            Integer settleType,
            LocalDate periodStart,
            LocalDate periodEnd,
            Integer isInvoice,
            String remark) {
    }

    private record SettlementAmounts(
            BigDecimal saw,
            BigDecimal rewind,
            BigDecimal extra,
            BigDecimal noTax,
            BigDecimal tax,
            BigDecimal total,
            List<SettleDetail> details) {
    }
}
