package com.paper.mes.settle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.PageResult;
import com.paper.mes.common.PageRequestBounds;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.machine.entity.Machine;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.oplog.entity.OperationLog;
import com.paper.mes.oplog.mapper.OperationLogMapper;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessStageOutputMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.settle.dto.ReceiveDTO;
import com.paper.mes.settle.dto.SettleActionReasonDTO;
import com.paper.mes.settle.dto.SettleByMonthDTO;
import com.paper.mes.settle.dto.SettleByOrderDTO;
import com.paper.mes.settle.dto.SettleByOrdersDTO;
import com.paper.mes.settle.dto.SettleCandidateQuery;
import com.paper.mes.settle.dto.SettleCandidateVO;
import com.paper.mes.settle.dto.SettleDetailVO;
import com.paper.mes.settle.dto.SettlePrintLineVO;
import com.paper.mes.settle.dto.SettleQuery;
import com.paper.mes.settle.dto.SettleQuoteVO;
import com.paper.mes.settle.entity.ReceiveRecord;
import com.paper.mes.settle.entity.SettleDetail;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.ReceiveRecordMapper;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import com.paper.mes.settle.service.SettleCandidateStatsLoader;
import com.paper.mes.settle.service.SettleCandidateAmountLoader;
import com.paper.mes.settle.service.SettleExportService;
import com.paper.mes.settle.service.SettleReceiveStatusResolver;
import com.paper.mes.settle.service.SettleService;
import com.paper.mes.settle.service.SettlementAmountCalculator;
import com.paper.mes.system.config.constant.NoRuleBizType;
import com.paper.mes.system.config.service.DocumentNoService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettleServiceImpl extends ServiceImpl<SettleOrderMapper, SettleOrder>
        implements SettleService {

    private static final int ORDER_STATUS_FINISHED = 4;
    private static final int ORDER_STATUS_SETTLED = 5;

    private static final int SETTLE_TYPE_BY_ORDER = 1;
    private static final int SETTLE_TYPE_BY_MONTH = 2;

    private static final int SETTLE_STATUS_PENDING = 1;
    private static final int SETTLE_STATUS_VOID = 4;
    private static final int RECEIVE_STATUS_ACTIVE = 1;
    private static final int RECEIVE_STATUS_CANCELLED = 2;

    private static final int STEP_TYPE_SAW = 1;
    private static final int STEP_TYPE_REWIND = 2;
    private static final int ROLL_NO_VOID = 3;
    private static final int IS_SPARE_YES = 1;
    private static final int IS_REMAIN_YES = 1;
    private static final int MONEY_SCALE = 2;

    private final SettleDetailMapper settleDetailMapper;
    private final ReceiveRecordMapper receiveRecordMapper;
    private final OriginalRollMapper originalRollMapper;
    private final FinishRollMapper finishRollMapper;
    private final FinishOriginalRelMapper finishOriginalRelMapper;
    private final ProcessStepMapper processStepMapper;
    private final ProcessStageOutputMapper processStageOutputMapper;
    private final ProcessOrderService processOrderService;
    private final CustomerService customerService;
    private final MachineMapper machineMapper;
    private final OperationLogMapper operationLogMapper;
    private final OperationLogService operationLogService;
    private final SettleCandidateStatsLoader statsLoader;
    private final SettleCandidateAmountLoader candidateAmountLoader;
    private final SettlementAmountCalculator settlementAmountCalculator;
    private final SettlePageDataLoader pageDataLoader;
    private final SettleExportService settleExportService;
    private final DocumentNoService documentNoService;
    private final BusinessLockService businessLockService;
    private final ObjectMapper objectMapper;

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
        Page<SettleOrder> page = page(PageRequestBounds.of(query.getCurrent(), query.getSize()), wrapper);
        normalizePageAmountView(page.getRecords());
        return PageResult.of(page);
    }

    @Override
    public PageResult<SettleCandidateVO> listCandidates(SettleCandidateQuery query) {
        LambdaQueryWrapper<ProcessOrder> wrapper = new LambdaQueryWrapper<ProcessOrder>()
                .eq(ProcessOrder::getOrderStatus, ORDER_STATUS_FINISHED);
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(ProcessOrder::getOrderNo, keyword)
                    .or().like(ProcessOrder::getCustomerName, keyword));
        }
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
        Page<ProcessOrder> page = processOrderService.page(
                PageRequestBounds.of(query.getCurrent(), query.getSize()), wrapper);
        List<ProcessOrder> orders = page.getRecords();
        Map<String, SettleCandidateStatsLoader.CandidateStats> statsByOrder =
                statsLoader.load(orders.stream().map(ProcessOrder::getUuid).toList());
        Map<String, SettleCandidateAmountLoader.CandidateAmount> amountsByOrder =
                candidateAmountLoader.load(orders);
        List<SettleCandidateVO> result = new ArrayList<>(orders.size());
        for (ProcessOrder order : orders) {
            SettleCandidateAmountLoader.CandidateAmount amount = amountsByOrder.get(order.getUuid());
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
            vo.setSawAmount(amount == null ? BigDecimal.ZERO : amount.saw());
            vo.setRewindAmount(amount == null ? BigDecimal.ZERO : amount.rewind());
            vo.setExtraAmount(amount == null ? BigDecimal.ZERO : amount.extra());
            vo.setTotalAmount(amount == null ? BigDecimal.ZERO : amount.effectiveTotal());
            result.add(vo);
        }
        Page<SettleCandidateVO> resultPage = Page.of(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(result);
        return PageResult.of(resultPage);
    }

    @Override
    public SettleQuoteVO quoteByOrders(SettleByOrdersDTO dto) {
        ensureDistinctOrderUuids(dto.getOrderUuids());
        return quote(loadOrdersByUuid(dto.getOrderUuids()), dto.getIsInvoice());
    }

    @Override
    public SettleQuoteVO quoteByMonth(SettleByMonthDTO dto) {
        if (dto.getPeriodStart().isAfter(dto.getPeriodEnd())) {
            throw new BusinessException("账期开始日不能晚于结束日");
        }
        List<ProcessOrder> orders = findMonthlyOrders(dto.getCustomerUuid(), dto.getPeriodStart(), dto.getPeriodEnd());
        if (orders.isEmpty()) throw new BusinessException("该期间无可结算加工单");
        return quote(orders, dto.getIsInvoice());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createByOrder(SettleByOrderDTO dto) {
        businessLockService.lockProcessOrders(List.of(dto.getOrderUuid()));
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
        ensureDistinctOrderUuids(dto.getOrderUuids());
        businessLockService.lockProcessOrders(dto.getOrderUuids());
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
        if (dto.getPeriodStart().isAfter(dto.getPeriodEnd())) {
            throw new BusinessException("账期开始日不能晚于结束日");
        }
        Customer customer = customerService.getById(dto.getCustomerUuid());
        if (customer == null) {
            throw new BusinessException(ErrorCode.E002, "客户不存在");
        }
        List<ProcessOrder> orders = processOrderService.list(
                new LambdaQueryWrapper<ProcessOrder>()
                        .eq(ProcessOrder::getCustomerUuid, dto.getCustomerUuid())
                        .eq(ProcessOrder::getOrderStatus, ORDER_STATUS_FINISHED)
                        .ge(ProcessOrder::getOrderDate, dto.getPeriodStart())
                        .le(ProcessOrder::getOrderDate, dto.getPeriodEnd())
                        .orderByAsc(ProcessOrder::getOrderDate)
                        .orderByAsc(ProcessOrder::getOrderNo));
        if (orders.isEmpty()) {
            throw new BusinessException("该期间无可结算加工单");
        }
        List<String> orderUuids = orders.stream().map(ProcessOrder::getUuid).toList();
        businessLockService.lockProcessOrders(orderUuids);
        orders = loadOrdersByUuid(orderUuids);

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
        SettleOrder viewOrder = snapshotSettleOrder(order);
        List<SettleDetail> details = settleDetailMapper.selectList(
                new LambdaQueryWrapper<SettleDetail>()
                        .eq(SettleDetail::getIsDeleted, 0)
                        .eq(SettleDetail::getSettleUuid, uuid));
        List<SettleDetail> snapshotDetails = readSnapshotDetails(order.getSnapBill());
        details = snapshotDetails == null ? normalizeDetailsForInvoiceView(viewOrder, details) : snapshotDetails;
        applySettlementAmountView(viewOrder, details);
        List<ReceiveRecord> receives = receiveRecordMapper.selectList(
                new LambdaQueryWrapper<ReceiveRecord>()
                        .eq(ReceiveRecord::getIsDeleted, 0)
                        .eq(ReceiveRecord::getSettleUuid, uuid)
                        .orderByAsc(ReceiveRecord::getReceiveDate));
        SettleDetailVO vo = new SettleDetailVO();
        vo.setOrder(viewOrder);
        vo.setDetails(details);
        vo.setReceives(receives);
        List<SettlePrintLineVO> snapshotLines = readSnapshotPrintLines(order.getSnapBill());
        List<SettlePrintLineVO> printLines = snapshotLines == null ? buildPrintLines(viewOrder, details) : snapshotLines;
        SettleFeeLineBuilder.ensureFeeLines(printLines);
        vo.setPrintLines(printLines);
        vo.setOperationLogs(loadOperationLogs(uuid));
        return vo;
    }

    @Override
    public void exportDetail(String uuid, HttpServletResponse response) {
        SettleDetailVO detail = getDetail(uuid);
        String filename = "结算单_" + detail.getOrder().getSettleNo() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(filename));
        try (Workbook workbook = settleExportService.buildWorkbook(detail)) {
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            throw new BusinessException("导出结算单失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(String uuid, ReceiveDTO dto) {
        if (!StringUtils.hasText(dto.getRequestId())) {
            throw new BusinessException("收款请求号不能为空");
        }
        String requestId = dto.getRequestId().trim();
        businessLockService.lockSettleOrder(uuid);
        SettleOrder settle = getById(uuid);
        if (settle == null) {
            throw new BusinessException(ErrorCode.E002, "结算单不存在");
        }
        ensureActiveSettle(settle);
        if (receiveRequestExists(uuid, requestId)) {
            return;
        }
        applySettlementAmountView(settle, normalizeDetailsForInvoiceView(settle, settleDetails(uuid)));
        if (settle.getUnreceivedAmount() != null && settle.getUnreceivedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("结算单已结清，不可再收款");
        }
        BigDecimal unreceived = nz(settle.getUnreceivedAmount());
        SettleReceiveAmountResolver.Resolved amount = SettleReceiveAmountResolver.resolve(dto, unreceived);

        ReceiveRecord record = new ReceiveRecord();
        record.setSettleUuid(uuid);
        record.setRequestId(requestId);
        record.setReceiveDate(dto.getReceiveDate() != null ? dto.getReceiveDate() : LocalDateTime.now());
        record.setReceiveAmount(amount.receiveAmount());
        record.setCashAmount(amount.cashAmount());
        record.setScrapOffsetAmount(amount.scrapOffsetAmount());
        record.setDiscountAmount(amount.discountAmount());
        record.setScrapWeight(amount.scrapWeight());
        record.setScrapUnitPrice(amount.scrapUnitPrice());
        record.setReceiveType(amount.receiveType());
        record.setPayMethod(amount.cashAmount().signum() > 0 ? dto.getPayMethod() : null);
        record.setPayNo(dto.getPayNo());
        String operator = resolveOperator(dto.getOperator());
        record.setOperator(operator);
        record.setRecordStatus(RECEIVE_STATUS_ACTIVE);
        record.setRemark(dto.getRemark());
        insertReceiveRecord(record);

        refreshReceiveState(settle);

        operationLogService.record(OperationLogService.BIZ_TYPE_SETTLE,
                settle.getUuid(), settle.getSettleNo(),
                OperationLogService.ACTION_RECEIVE, null,
                receiveLogText("登记收款", amount) + "，业务经办人：" + operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelReceive(String uuid, String receiveUuid, SettleActionReasonDTO dto) {
        businessLockService.lockSettleOrder(uuid);
        businessLockService.lockReceiveRecord(receiveUuid);
        SettleOrder settle = requireSettle(uuid);
        ensureActiveSettle(settle);
        ReceiveRecord record = receiveRecordMapper.selectById(receiveUuid);
        if (record == null || !uuid.equals(record.getSettleUuid())) {
            throw new BusinessException(ErrorCode.E002, "收款流水不存在");
        }
        if (record.getRecordStatus() != null && record.getRecordStatus() == RECEIVE_STATUS_CANCELLED) {
            throw new BusinessException("该收款流水已撤销");
        }
        String operator = AuthContextHolder.currentDisplayName();
        record.setRecordStatus(RECEIVE_STATUS_CANCELLED);
        record.setCancelTime(LocalDateTime.now());
        record.setCancelBy(operator);
        record.setCancelReason(dto.getReason());
        updateReceiveRecordForCancel(record);

        refreshReceiveState(settle);
        operationLogService.record(OperationLogService.BIZ_TYPE_SETTLE,
                settle.getUuid(), settle.getSettleNo(),
                OperationLogService.ACTION_RECEIVE_CANCEL, operator,
                cancelReceiveLogText(record, dto.getReason()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidSettle(String uuid, SettleActionReasonDTO dto) {
        businessLockService.lockSettleOrder(uuid);
        SettleOrder settle = requireSettle(uuid);
        ensureActiveSettle(settle);
        if (activeReceiveAmount(uuid).compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("已有有效收款，需先撤销收款后再作废结算单");
        }
        List<SettleDetail> details = settleDetails(uuid);
        businessLockService.lockProcessOrders(details.stream().map(SettleDetail::getOrderUuid).toList());
        Map<String, ProcessOrder> orderByUuid = loadSettleDetailOrders(details);
        for (SettleDetail detail : details) {
            ProcessOrder order = orderByUuid.get(detail.getOrderUuid());
            if (order != null && order.getOrderStatus() != null && order.getOrderStatus() == ORDER_STATUS_SETTLED) {
                rollbackSettledProcessOrder(order.getUuid());
            }
            ConcurrencyGuard.requireRowUpdated(settleDetailMapper.delete(new LambdaQueryWrapper<SettleDetail>()
                    .eq(SettleDetail::getUuid, detail.getUuid())
                    .eq(SettleDetail::getSettleUuid, uuid)));
        }
        markSettleVoided(settle, dto.getReason());
        operationLogService.record(OperationLogService.BIZ_TYPE_SETTLE,
                settle.getUuid(), settle.getSettleNo(),
                OperationLogService.ACTION_SETTLE_VOID, AuthContextHolder.currentDisplayName(),
                "作废结算单，原因：" + dto.getReason());
    }

    private List<SettleDetail> normalizeDetailsForInvoiceView(SettleOrder settle, List<SettleDetail> details) {
        if (details.isEmpty()) {
            return details;
        }
        List<String> orderUuids = details.stream().map(SettleDetail::getOrderUuid).toList();
        Map<String, ProcessOrder> orderByUuid = new LinkedHashMap<>();
        for (ProcessOrder order : processOrderService.listByIds(orderUuids)) {
            orderByUuid.put(order.getUuid(), order);
        }
        Customer customer = customerService.getById(settle.getCustomerUuid());
        List<SettleDetail> normalized = new ArrayList<>(details.size());
        for (SettleDetail detail : details) {
            ProcessOrder order = orderByUuid.get(detail.getOrderUuid());
            normalized.add(order == null ? detail : normalizedDetail(detail, order, settle.getIsInvoice(), customer));
        }
        return normalized;
    }

    private SettleDetail normalizedDetail(SettleDetail source, ProcessOrder order, Integer isInvoice, Customer customer) {
        SettleDetail detail = new SettleDetail();
        detail.setUuid(source.getUuid());
        detail.setSettleUuid(source.getSettleUuid());
        detail.setOrderUuid(source.getOrderUuid());
        detail.setOrderNo(source.getOrderNo());
        detail.setSawAmount(source.getSawAmount());
        detail.setRewindAmount(source.getRewindAmount());
        detail.setExtraAmount(source.getExtraAmount());
        BigDecimal fallbackAmount = invoiceTotal(detailBaseAmount(detail), isInvoice, taxRateOf(order, customer));
        detail.setOrderAmount(settleOrderAmount(order, fallbackAmount));
        detail.setRemark(source.getRemark());
        return detail;
    }

    private void applySettlementAmountView(SettleOrder settle, List<SettleDetail> details) {
        applySettlementAmountView(settle, details, activeReceiveTotals(settle.getUuid()));
    }

    private void applySettlementAmountView(SettleOrder settle, List<SettleDetail> details,
                                           SettleReceiveTotals receiveTotals) {
        SettleAmountSnapshotReader.Amounts amounts =
                SettleAmountSnapshotReader.resolve(settle, details, objectMapper);
        settle.setAmountNoTax(amounts.noTax());
        settle.setTaxAmount(amounts.tax());
        settle.setTotalAmount(amounts.total());
        applyReceiveState(settle, amounts.total(), receiveTotals);
    }

    private SettleOrder requireSettle(String uuid) {
        SettleOrder settle = getById(uuid);
        if (settle == null) {
            throw new BusinessException(ErrorCode.E002, "结算单不存在");
        }
        return settle;
    }

    private void normalizePageAmountView(List<SettleOrder> records) {
        if (records.isEmpty()) {
            return;
        }
        SettlePageDataLoader.PageData data = pageDataLoader.load(records);
        for (SettleOrder settle : records) {
            List<SettleDetail> details = data.detailsBySettle().getOrDefault(settle.getUuid(), List.of());
            List<SettleDetail> normalized = normalizePageDetails(settle, details, data);
            SettleReceiveTotals totals = data.receiveTotalsBySettle()
                    .getOrDefault(settle.getUuid(), SettleReceiveTotals.zero());
            applySettlementAmountView(settle, normalized, totals);
        }
    }

    private List<SettleDetail> normalizePageDetails(SettleOrder settle, List<SettleDetail> details,
                                                    SettlePageDataLoader.PageData data) {
        Customer customer = data.customerByUuid().get(settle.getCustomerUuid());
        List<SettleDetail> normalized = new ArrayList<>(details.size());
        for (SettleDetail detail : details) {
            ProcessOrder order = data.orderByUuid().get(detail.getOrderUuid());
            normalized.add(order == null ? detail
                    : normalizedDetail(detail, order, settle.getIsInvoice(), customer));
        }
        return normalized;
    }

    private void refreshReceiveState(SettleOrder settle) {
        Integer previousStatus = settle.getSettleStatus();
        applySettlementAmountView(settle, normalizeDetailsForInvoiceView(settle, settleDetails(settle.getUuid())));
        BigDecimal total = nz(settle.getTotalAmount());
        applyReceiveState(settle, total, activeReceiveTotals(settle.getUuid()));
        updateSettleReceiveState(settle, previousStatus);
    }

    private void applyReceiveState(SettleOrder settle, BigDecimal totalAmount, BigDecimal receivedAmount) {
        applyReceiveState(settle, totalAmount,
                new SettleReceiveTotals(nz(receivedAmount), nz(receivedAmount),
                        BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private void applyReceiveState(SettleOrder settle, BigDecimal totalAmount, SettleReceiveTotals totals) {
        SettleReceiveStatusResolver.State state =
                SettleReceiveStatusResolver.resolve(totalAmount, totals.receiveAmount());
        settle.setReceivedAmount(state.receivedAmount());
        settle.setCashReceivedAmount(totals.cashAmount());
        settle.setScrapOffsetAmount(totals.scrapOffsetAmount());
        settle.setDiscountAmount(totals.discountAmount());
        settle.setUnreceivedAmount(state.unreceivedAmount());
        settle.setSettleStatus(state.status());
    }

    private void updateReceiveRecordForCancel(ReceiveRecord record) {
        ConcurrencyGuard.requireRowUpdated(receiveRecordMapper.update(null,
                new LambdaUpdateWrapper<ReceiveRecord>()
                        .eq(ReceiveRecord::getUuid, record.getUuid())
                        .eq(ReceiveRecord::getRecordStatus, RECEIVE_STATUS_ACTIVE)
                        .set(ReceiveRecord::getRecordStatus, RECEIVE_STATUS_CANCELLED)
                        .set(ReceiveRecord::getCancelTime, record.getCancelTime())
                        .set(ReceiveRecord::getCancelBy, record.getCancelBy())
                        .set(ReceiveRecord::getCancelReason, record.getCancelReason())
                        .set(ReceiveRecord::getUpdateTime, LocalDateTime.now())
                        .set(ReceiveRecord::getUpdateBy, currentOperator())
                        .setSql("version = version + 1")));
    }

    private void updateSettleReceiveState(SettleOrder settle, Integer previousStatus) {
        LambdaUpdateWrapper<SettleOrder> wrapper = new LambdaUpdateWrapper<SettleOrder>()
                .eq(SettleOrder::getUuid, settle.getUuid())
                .set(SettleOrder::getSawAmount, settle.getSawAmount())
                .set(SettleOrder::getRewindAmount, settle.getRewindAmount())
                .set(SettleOrder::getExtraAmount, settle.getExtraAmount())
                .set(SettleOrder::getAmountNoTax, settle.getAmountNoTax())
                .set(SettleOrder::getTaxAmount, settle.getTaxAmount())
                .set(SettleOrder::getTotalAmount, settle.getTotalAmount())
                .set(SettleOrder::getReceivedAmount, settle.getReceivedAmount())
                .set(SettleOrder::getCashReceivedAmount, settle.getCashReceivedAmount())
                .set(SettleOrder::getScrapOffsetAmount, settle.getScrapOffsetAmount())
                .set(SettleOrder::getDiscountAmount, settle.getDiscountAmount())
                .set(SettleOrder::getUnreceivedAmount, settle.getUnreceivedAmount())
                .set(SettleOrder::getSettleStatus, settle.getSettleStatus())
                .set(SettleOrder::getUpdateTime, LocalDateTime.now())
                .set(SettleOrder::getUpdateBy, currentOperator())
                .setSql("version = version + 1");
        if (previousStatus == null) {
            wrapper.isNull(SettleOrder::getSettleStatus);
        } else {
            wrapper.eq(SettleOrder::getSettleStatus, previousStatus);
        }
        ConcurrencyGuard.requireRowUpdated(getBaseMapper().update(null, wrapper));
    }

    private BigDecimal activeReceiveAmount(String settleUuid) {
        return activeReceiveTotals(settleUuid).receiveAmount();
    }

    private String receiveLogText(String action, SettleReceiveAmountResolver.Resolved amount) {
        return action + " " + amount.receiveAmount() + " 元，现金 "
                + amount.cashAmount() + " 元，废纸抵扣 "
                + amount.scrapOffsetAmount() + " 元，优惠核销 "
                + amount.discountAmount() + " 元，废纸 "
                + amount.scrapWeight() + " kg";
    }

    private String cancelReceiveLogText(ReceiveRecord record, String reason) {
        return "撤销收款 " + nz(record.getReceiveAmount()) + " 元，现金 "
                + nz(record.getCashAmount()) + " 元，废纸抵扣 "
                + nz(record.getScrapOffsetAmount()) + " 元，优惠核销 "
                + nz(record.getDiscountAmount()) + " 元，原因：" + reason;
    }

    private SettleReceiveTotals activeReceiveTotals(String settleUuid) {
        List<ReceiveRecord> receives = receiveRecordMapper.selectList(
                new LambdaQueryWrapper<ReceiveRecord>()
                        .eq(ReceiveRecord::getIsDeleted, 0)
                        .eq(ReceiveRecord::getSettleUuid, settleUuid));
        SettleReceiveTotals totals = SettleReceiveTotals.zero();
        for (ReceiveRecord record : receives) {
            if (record.getRecordStatus() == null || record.getRecordStatus() == RECEIVE_STATUS_ACTIVE) {
                totals = totals.add(record);
            }
        }
        return totals;
    }

    private List<SettleDetail> settleDetails(String settleUuid) {
        return settleDetailMapper.selectList(new LambdaQueryWrapper<SettleDetail>()
                .eq(SettleDetail::getIsDeleted, 0)
                .eq(SettleDetail::getSettleUuid, settleUuid));
    }

    private Map<String, ProcessOrder> loadSettleDetailOrders(List<SettleDetail> details) {
        List<String> orderUuids = details.stream()
                .map(SettleDetail::getOrderUuid)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (orderUuids.isEmpty()) {
            return Map.of();
        }
        List<ProcessOrder> orders = processOrderService.listByIds(orderUuids);
        Map<String, ProcessOrder> orderByUuid = new LinkedHashMap<>();
        for (ProcessOrder order : orders) {
            orderByUuid.put(order.getUuid(), order);
        }
        return orderByUuid;
    }

    private List<OperationLog> loadOperationLogs(String settleUuid) {
        return operationLogMapper.selectList(new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getBizType, OperationLogService.BIZ_TYPE_SETTLE)
                .eq(OperationLog::getBizUuid, settleUuid)
                .orderByDesc(OperationLog::getOperateTime));
    }

    private List<SettlePrintLineVO> buildPrintLines(SettleOrder settle, List<SettleDetail> details) {
        if (details.isEmpty()) {
            return List.of();
        }
        List<String> orderUuids = details.stream().map(SettleDetail::getOrderUuid).toList();
        List<ProcessOrder> orders = processOrderService.listByIds(orderUuids);
        Map<String, ProcessOrder> orderByUuid = new LinkedHashMap<>();
        for (ProcessOrder order : orders) {
            orderByUuid.put(order.getUuid(), order);
        }
        List<OriginalRoll> rolls = originalRollMapper.selectList(new LambdaQueryWrapper<OriginalRoll>()
                .in(OriginalRoll::getOrderUuid, orderUuids)
                .orderByAsc(OriginalRoll::getOrderUuid)
                .orderByAsc(OriginalRoll::getRowSort));
        List<ProcessStep> steps = processStepMapper.selectList(new LambdaQueryWrapper<ProcessStep>()
                .in(ProcessStep::getOrderUuid, orderUuids));
        List<ProcessStageOutput> stageOutputs = processStageOutputMapper.selectList(new LambdaQueryWrapper<ProcessStageOutput>()
                .in(ProcessStageOutput::getOrderUuid, orderUuids)
                .eq(ProcessStageOutput::getIsDeleted, 0)
                .orderByAsc(ProcessStageOutput::getOriginalUuid)
                .orderByAsc(ProcessStageOutput::getStageLevel)
                .orderByAsc(ProcessStageOutput::getOutputSort));
        List<FinishRoll> finishes = finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .in(FinishRoll::getOrderUuid, orderUuids));
        List<FinishOriginalRel> rels = finishOriginalRelMapper.selectList(new LambdaQueryWrapper<FinishOriginalRel>()
                .in(FinishOriginalRel::getOrderUuid, orderUuids));

        Map<String, List<ProcessStep>> stepsByOriginal = groupStepsByOriginal(steps);
        Map<String, List<ProcessStageOutput>> outputsByOriginal = groupStageOutputsByOriginal(stageOutputs);
        Map<String, List<FinishRoll>> finishesByOriginal = groupFinishesByOriginal(finishes, rels, rolls);
        Map<String, String> machineNameByUuid = loadMachineNames(rolls);
        Map<String, SettleDetail> detailByOrderUuid = new LinkedHashMap<>();
        for (SettleDetail detail : details) {
            detailByOrderUuid.put(detail.getOrderUuid(), detail);
        }
        Customer customer = customerService.getById(settle.getCustomerUuid());
        Map<String, List<SettlePrintLineVO>> linesByOrderUuid = new LinkedHashMap<>();
        for (OriginalRoll roll : rolls) {
            ProcessOrder order = orderByUuid.get(roll.getOrderUuid());
            SettlePrintLineVO line = buildPrintLine(settle, order, customer, roll,
                    stepsByOriginal.getOrDefault(roll.getUuid(), List.of()),
                    outputsByOriginal.getOrDefault(roll.getUuid(), List.of()),
                    finishesByOriginal.getOrDefault(roll.getUuid(), List.of()), machineNameByUuid);
            linesByOrderUuid.computeIfAbsent(roll.getOrderUuid(), k -> new ArrayList<>()).add(line);
        }
        List<SettlePrintLineVO> lines = new ArrayList<>(rolls.size());
        for (Map.Entry<String, List<SettlePrintLineVO>> entry : linesByOrderUuid.entrySet()) {
            SettleDetail detail = detailByOrderUuid.get(entry.getKey());
            applyOrderAmountClosure(entry.getValue(), detail, orderByUuid.get(entry.getKey()));
            lines.addAll(entry.getValue());
        }
        return lines;
    }

    private Map<String, List<ProcessStep>> groupStepsByOriginal(List<ProcessStep> steps) {
        Map<String, List<ProcessStep>> grouped = new LinkedHashMap<>();
        for (ProcessStep step : steps) {
            if (StringUtils.hasText(step.getOriginalUuid())) {
                grouped.computeIfAbsent(step.getOriginalUuid(), k -> new ArrayList<>()).add(step);
            }
        }
        return grouped;
    }

    private Map<String, List<ProcessStageOutput>> groupStageOutputsByOriginal(List<ProcessStageOutput> outputs) {
        Map<String, List<ProcessStageOutput>> grouped = new LinkedHashMap<>();
        for (ProcessStageOutput output : outputs) {
            if (StringUtils.hasText(output.getOriginalUuid())) {
                grouped.computeIfAbsent(output.getOriginalUuid(), k -> new ArrayList<>()).add(output);
            }
        }
        return grouped;
    }

    private Map<String, List<FinishRoll>> groupFinishesByOriginal(List<FinishRoll> finishes,
                                                                  List<FinishOriginalRel> rels,
                                                                  List<OriginalRoll> rolls) {
        Map<String, FinishRoll> finishByUuid = new LinkedHashMap<>();
        for (FinishRoll finish : finishes) {
            finishByUuid.put(finish.getUuid(), finish);
        }
        Map<String, List<FinishRoll>> grouped = new LinkedHashMap<>();
        for (FinishOriginalRel rel : rels) {
            FinishRoll finish = finishByUuid.get(rel.getFinishUuid());
            if (finish != null) {
                grouped.computeIfAbsent(rel.getOriginalUuid(), k -> new ArrayList<>()).add(finish);
            }
        }
        for (OriginalRoll roll : rolls) {
            String label = finishOriginalKey(roll);
            for (FinishRoll finish : finishes) {
                if (label.equals(finish.getOriginalRollNos())) {
                    grouped.computeIfAbsent(roll.getUuid(), k -> new ArrayList<>()).add(finish);
                }
            }
        }
        for (Map.Entry<String, List<FinishRoll>> entry : grouped.entrySet()) {
            entry.setValue(uniqueFinishes(entry.getValue()));
        }
        return grouped;
    }

    private List<FinishRoll> uniqueFinishes(List<FinishRoll> finishes) {
        Map<String, FinishRoll> unique = new LinkedHashMap<>();
        for (FinishRoll finish : finishes) {
            String key = finish.getUuid() != null ? finish.getUuid() : finish.getFinishRollNo();
            unique.put(key == null ? String.valueOf(unique.size()) : key, finish);
        }
        return new ArrayList<>(unique.values());
    }

    private Map<String, String> loadMachineNames(List<OriginalRoll> rolls) {
        List<String> machineUuids = rolls.stream()
                .map(OriginalRoll::getMachineUuid)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (machineUuids.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return machineMapper.selectBatchIds(machineUuids).stream()
                .collect(java.util.stream.Collectors.toMap(Machine::getUuid,
                        machine -> machine.getMachineName() == null ? "" : machine.getMachineName(),
                        (left, right) -> left));
    }

    private SettlePrintLineVO buildPrintLine(SettleOrder settle, ProcessOrder order, Customer customer, OriginalRoll roll,
                                             List<ProcessStep> steps, List<ProcessStageOutput> stageOutputs,
                                             List<FinishRoll> finishes,
                                             Map<String, String> machineNameByUuid) {
        LineAmounts amounts = lineAmounts(steps);
        BigDecimal taxRate = taxRateOf(order, customer);
        List<FinishRoll> deliverableFinishes = finishes.stream().filter(this::isDeliverableFinish).toList();
        SettlePrintLineVO line = new SettlePrintLineVO();
        line.setSettleUuid(settle.getUuid());
        line.setOrderUuid(roll.getOrderUuid());
        line.setOrderNo(order == null ? roll.getOrderNo() : order.getOrderNo());
        line.setOrderDate(order == null ? null : order.getOrderDate());
        line.setOriginalUuid(roll.getUuid());
        line.setOriginalLabel(originalLabel(roll));
        line.setOriginalRollNo(roll.getRollNo());
        line.setOriginalExtraNo(roll.getExtraNo());
        line.setPaperName(roll.getPaperName());
        line.setGramWeight(roll.getGramWeight());
        line.setActualGramWeight(roll.getActualGramWeight());
        line.setOriginalWidth(roll.getOriginalWidth());
        line.setActualWidth(roll.getActualWidth());
        line.setOriginalDiameter(roll.getOriginalDiameter());
        line.setCoreDiameter(roll.getCoreDiameter());
        line.setOriginalLength(roll.getOriginalLength());
        line.setOriginalWeight(originalWeight(roll));
        line.setProcessMode(roll.getProcessMode());
        line.setMainStepType(roll.getMainStepType());
        line.setMachineUuid(roll.getMachineUuid());
        line.setMachineName(resolveMachineName(machineNameByUuid, roll.getMachineUuid()));
        line.setProcessText(processText(roll));
        line.setProcessStepSummary(processStepSummary(steps));
        line.setFinishSummary(finishSummary(deliverableFinishes));
        line.setFinishDetailSummary(finishDetailSummary(deliverableFinishes));
        line.setFinishCount(deliverableFinishes.size());
        line.setFinishWeight(sumFinishWeight(deliverableFinishes));
        line.setTrimWeight(sumTrimWeight(finishes));
        line.setTrimSummary(trimSummary(finishes));
        line.setSawWeight(amounts.sawWeight());
        line.setRewindWeight(amounts.rewindWeight());
        line.setSawUnitPrice(amounts.sawUnitPrice());
        line.setSawInvoiceUnitPrice(invoiceUnitPrice(amounts.sawUnitPrice(), settle.getIsInvoice(), taxRate));
        line.setRewindUnitPrice(amounts.rewindUnitPrice());
        line.setRewindInvoiceUnitPrice(invoiceUnitPrice(amounts.rewindUnitPrice(), settle.getIsInvoice(), taxRate));
        line.setSawAmount(amounts.sawAmount());
        line.setRewindAmount(amounts.rewindAmount());
        line.setProcessAmount(amounts.processAmount());
        line.setExtraAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        line.setTaxAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        line.setTaxRate(taxRate);
        line.setLineAmount(amounts.processAmount());
        line.setIsInvoice(settle.getIsInvoice());
        line.setRemark(roll.getRemark());
        line.setFeeLines(SettleFeeLineBuilder.fromSteps(line, roll, steps, stageOutputs));
        return line;
    }

    private String resolveMachineName(Map<String, String> machineNameByUuid, String machineUuid) {
        if (!StringUtils.hasText(machineUuid) || machineNameByUuid == null || machineNameByUuid.isEmpty()) {
            return null;
        }
        return machineNameByUuid.get(machineUuid);
    }

    private void applyOrderAmountClosure(List<SettlePrintLineVO> lines, SettleDetail detail, ProcessOrder order) {
        if (lines.isEmpty()) {
            return;
        }
        BigDecimal extraTotal = detail == null ? BigDecimal.ZERO : nz(detail.getExtraAmount());
        applyExtraAmount(lines, extraTotal, extraFeeSummary(order, extraTotal));
        BigDecimal targetAmount = detail == null ? sumLineAmount(lines) : nz(detail.getOrderAmount());
        BigDecimal invoiceIncrease = targetAmount.subtract(sumLineAmount(lines));
        applyInvoiceIncrease(lines, invoiceIncrease);
    }

    private void applyExtraAmount(List<SettlePrintLineVO> lines, BigDecimal extraTotal, String extraFeeSummary) {
        if (extraTotal.signum() == 0) {
            return;
        }
        BigDecimal assigned = BigDecimal.ZERO;
        BigDecimal weightTotal = sumOriginalWeight(lines);
        for (int i = 0; i < lines.size(); i++) {
            SettlePrintLineVO line = lines.get(i);
            BigDecimal share = i == lines.size() - 1
                    ? extraTotal.subtract(assigned)
                    : proportionalShare(extraTotal, nz(line.getOriginalWeight()), weightTotal, lines.size());
            assigned = assigned.add(share);
            line.setExtraAmount(share);
            line.setExtraFeeSummary(extraFeeSummary);
            line.setLineAmount(nz(line.getLineAmount()).add(share));
            SettleFeeLineBuilder.appendExtraLine(line, share, extraFeeSummary);
        }
    }

    private String extraFeeSummary(ProcessOrder order, BigDecimal extraTotal) {
        if (order == null || extraTotal.signum() == 0) {
            return null;
        }
        List<String> items = new ArrayList<>();
        appendFee(items, "加急费", order.getUrgentFee());
        appendFee(items, "托盘费", order.getPalletFee());
        appendFee(items, "装卸费", order.getLoadingFee());
        appendFee(items, "运费", order.getFreightFee());
        appendFee(items, "其他费", order.getOtherFee());
        return items.isEmpty() ? "额外费用合计 " + moneyText(extraTotal) : String.join("；", items);
    }

    private void appendFee(List<String> items, String label, BigDecimal amount) {
        if (amount != null && amount.signum() != 0) {
            items.add(label + " " + moneyText(amount));
        }
    }

    private String moneyText(BigDecimal amount) {
        return nz(amount).setScale(MONEY_SCALE, RoundingMode.HALF_UP).toPlainString();
    }

    private void applyInvoiceIncrease(List<SettlePrintLineVO> lines, BigDecimal invoiceIncrease) {
        if (invoiceIncrease.signum() == 0) {
            return;
        }
        BigDecimal assigned = BigDecimal.ZERO;
        BigDecimal baseTotal = sumLineAmount(lines);
        for (int i = 0; i < lines.size(); i++) {
            SettlePrintLineVO line = lines.get(i);
            BigDecimal share = i == lines.size() - 1
                    ? invoiceIncrease.subtract(assigned)
                    : proportionalShare(invoiceIncrease, nz(line.getLineAmount()), baseTotal, lines.size());
            assigned = assigned.add(share);
            line.setTaxAmount(share);
            line.setLineAmount(nz(line.getLineAmount()).add(share));
            SettleFeeLineBuilder.appendTaxLine(line, share);
        }
    }

    private BigDecimal proportionalShare(BigDecimal total, BigDecimal base, BigDecimal baseTotal, int count) {
        if (baseTotal.signum() <= 0) {
            return total.divide(BigDecimal.valueOf(count), MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return total.multiply(base).divide(baseTotal, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal sumOriginalWeight(List<SettlePrintLineVO> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (SettlePrintLineVO line : lines) {
            total = total.add(nz(line.getOriginalWeight()));
        }
        return total;
    }

    private BigDecimal sumLineAmount(List<SettlePrintLineVO> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (SettlePrintLineVO line : lines) {
            total = total.add(nz(line.getLineAmount()));
        }
        return total;
    }

    private LineAmounts lineAmounts(List<ProcessStep> steps) {
        BigDecimal sawWeight = BigDecimal.ZERO;
        BigDecimal rewindWeight = BigDecimal.ZERO;
        BigDecimal sawAmount = BigDecimal.ZERO;
        BigDecimal rewindAmount = BigDecimal.ZERO;
        BigDecimal sawUnitPrice = null;
        BigDecimal rewindUnitPrice = null;
        for (ProcessStep step : steps) {
            if (step.getStepType() != null && step.getStepType() == STEP_TYPE_SAW) {
                sawWeight = sawWeight.add(nz(step.getProcessWeight()));
                sawAmount = sawAmount.add(nz(step.getStepAmount()));
                sawUnitPrice = firstNonNull(sawUnitPrice, step.getUnitPrice());
            } else if (step.getStepType() != null && step.getStepType() == STEP_TYPE_REWIND) {
                rewindWeight = rewindWeight.add(nz(step.getProcessWeight()));
                rewindAmount = rewindAmount.add(nz(step.getStepAmount()));
                rewindUnitPrice = firstNonNull(rewindUnitPrice, step.getUnitPrice());
            }
        }
        return new LineAmounts(sawWeight, rewindWeight, sawUnitPrice, rewindUnitPrice, sawAmount, rewindAmount);
    }

    private String processStepSummary(List<ProcessStep> steps) {
        if (steps.isEmpty()) {
            return "-";
        }
        return steps.stream()
                .map(this::processStepText)
                .distinct()
                .reduce((left, right) -> left + "；" + right)
                .orElse("-");
    }

    private String processStepText(ProcessStep step) {
        String name = StringUtils.hasText(step.getStepName()) ? step.getStepName() : stepTypeText(step.getStepType());
        List<String> parts = new ArrayList<>();
        if (step.getKnifeCount() != null) {
            parts.add(step.getKnifeCount() + "刀");
        }
        if (step.getProcessWeight() != null) {
            parts.add(processWeightText(step));
        }
        if (step.getUnitPrice() != null) {
            parts.add("单价 " + moneyText(step.getUnitPrice()));
        }
        return parts.isEmpty() ? name : name + "（" + String.join(" / ", parts) + "）";
    }

    private String processWeightText(ProcessStep step) {
        if (step.getStepType() != null && step.getStepType() == STEP_TYPE_REWIND) {
            BigDecimal quantity = SettleFeeLineSupport.billingQuantity(step.getStepAmount(), step.getUnitPrice(), step.getProcessWeight());
            return quantity.stripTrailingZeros().toPlainString() + "t";
        }
        return nz(step.getProcessWeight()).stripTrailingZeros().toPlainString() + "kg";
    }

    private String stepTypeText(Integer stepType) {
        if (stepType != null && stepType == STEP_TYPE_SAW) {
            return "锯纸";
        }
        if (stepType != null && stepType == STEP_TYPE_REWIND) {
            return "复卷";
        }
        return "工序";
    }

    private BigDecimal firstNonNull(BigDecimal current, BigDecimal next) {
        return current != null ? current : next;
    }

    private String finishOriginalKey(OriginalRoll roll) {
        return StringUtils.hasText(roll.getRollNo()) ? roll.getRollNo() : roll.getUuid();
    }

    private String originalLabel(OriginalRoll roll) {
        if (StringUtils.hasText(roll.getRollNo())) {
            return roll.getRollNo();
        }
        if (StringUtils.hasText(roll.getExtraNo())) {
            return roll.getExtraNo();
        }
        return "母卷 " + roll.getRowSort();
    }

    private BigDecimal originalWeight(OriginalRoll roll) {
        if (roll.getActualWeight() != null) {
            return roll.getActualWeight();
        }
        if (roll.getTotalWeight() != null) {
            return roll.getTotalWeight();
        }
        return nz(roll.getRollWeight()).multiply(BigDecimal.valueOf(roll.getPieceNum() == null ? 1 : roll.getPieceNum()));
    }

    private String processText(OriginalRoll roll) {
        if (roll.getProcessMode() != null && roll.getProcessMode() == 3) {
            return "直发";
        }
        if (roll.getMainStepType() != null && roll.getMainStepType() == STEP_TYPE_REWIND) {
            return "复卷";
        }
        return "锯纸";
    }

    private String finishSummary(List<FinishRoll> finishes) {
        if (finishes.isEmpty()) {
            return "-";
        }
        return finishes.stream()
                .limit(5)
                .map(f -> f.getFinishRollNo() == null ? "-" : f.getFinishRollNo())
                .reduce((a, b) -> a + "、" + b)
                .orElse("-");
    }

    private String finishDetailSummary(List<FinishRoll> finishes) {
        if (finishes.isEmpty()) {
            return "-";
        }
        return finishes.stream()
                .limit(12)
                .map(this::finishDetailText)
                .reduce((a, b) -> a + "；" + b)
                .orElse("-");
    }

    private String finishDetailText(FinishRoll finish) {
        List<String> spec = new ArrayList<>();
        if (finish.getFinishWidth() != null) {
            spec.add(finish.getFinishWidth() + "mm");
        }
        if (finish.getFinishDiameter() != null) {
            spec.add("φ" + finish.getFinishDiameter());
        }
        if (finish.getActualWeight() != null) {
            spec.add(nz(finish.getActualWeight()) + "kg");
        }
        String no = StringUtils.hasText(finish.getFinishRollNo()) ? finish.getFinishRollNo() : "-";
        return spec.isEmpty() ? no : no + "（" + String.join(" / ", spec) + "）";
    }

    private String trimSummary(List<FinishRoll> finishes) {
        int trimWidth = 0;
        BigDecimal trimWeight = BigDecimal.ZERO;
        for (FinishRoll finish : finishes) {
            if (isSpareFinish(finish) || isVoidFinish(finish)) {
                continue;
            }
            if (isRemainFinish(finish)) {
                trimWidth += finish.getFinishWidth() == null ? 0 : finish.getFinishWidth();
                trimWeight = trimWeight.add(finishWeight(finish));
            } else {
                trimWidth += finish.getTrimWidthShare() == null ? 0 : finish.getTrimWidthShare();
                trimWeight = trimWeight.add(nz(finish.getTrimWeightShare()));
            }
        }
        if (trimWidth == 0 && trimWeight.signum() == 0) {
            return "-";
        }
        return trimWidth + "mm / " + trimWeight + "kg";
    }

    private BigDecimal sumFinishWeight(List<FinishRoll> finishes) {
        BigDecimal total = BigDecimal.ZERO;
        for (FinishRoll finish : finishes) {
            if (isDeliverableFinish(finish)) {
                total = total.add(finishWeight(finish));
            }
        }
        return total;
    }

    private BigDecimal sumTrimWeight(List<FinishRoll> finishes) {
        BigDecimal total = BigDecimal.ZERO;
        for (FinishRoll finish : finishes) {
            if (isSpareFinish(finish) || isVoidFinish(finish)) {
                continue;
            }
            total = total.add(isRemainFinish(finish) ? finishWeight(finish) : nz(finish.getTrimWeightShare()));
        }
        return total;
    }

    private boolean isDeliverableFinish(FinishRoll finish) {
        return !isSpareFinish(finish) && !isRemainFinish(finish)
                && !isVoidFinish(finish);
    }

    private boolean isSpareFinish(FinishRoll finish) {
        return IS_SPARE_YES == (finish.getIsSpare() == null ? 0 : finish.getIsSpare());
    }

    private boolean isRemainFinish(FinishRoll finish) {
        return IS_REMAIN_YES == (finish.getIsRemain() == null ? 0 : finish.getIsRemain());
    }

    private boolean isVoidFinish(FinishRoll finish) {
        return ROLL_NO_VOID == (finish.getRollNoStatus() == null ? 0 : finish.getRollNoStatus());
    }

    private BigDecimal finishWeight(FinishRoll finish) {
        return nz(finish.getActualWeight() != null ? finish.getActualWeight() : finish.getEstimateWeight());
    }

    private String contentDisposition(String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename*=UTF-8''" + encoded;
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
        settle.setRemark(context.remark());

        SettlementAmounts amounts = sumAmounts(context.orders(), settle.getIsInvoice(), customer);
        settle.setSawAmount(amounts.saw());
        settle.setRewindAmount(amounts.rewind());
        settle.setExtraAmount(amounts.extra());
        settle.setAmountNoTax(amounts.noTax());
        settle.setTaxAmount(amounts.tax());
        settle.setTotalAmount(amounts.total());
        applyReceiveState(settle, amounts.total(), BigDecimal.ZERO);
        ConcurrencyGuard.requireUpdated(save(settle));

        for (SettleDetail detail : amounts.details()) {
            detail.setSettleUuid(settle.getUuid());
            ensureOrderNotSettled(detail.getOrderUuid());
            insertSettleDetail(detail);
            processOrderService.changeStatus(detail.getOrderUuid(), ORDER_STATUS_SETTLED, null);
        }
        settle.setSnapBill(buildSettleSnapshot(settle, amounts.details(), context.orders()));
        settle.setSnapBillTime(LocalDateTime.now());
        updateSettleSnapshot(settle);
        return settle.getUuid();
    }

    private void ensureOrderNotSettled(String orderUuid) {
        long count = settleDetailMapper.selectCount(new LambdaQueryWrapper<SettleDetail>()
                .eq(SettleDetail::getIsDeleted, 0)
                .eq(SettleDetail::getOrderUuid, orderUuid));
        if (count > 0) {
            throw new BusinessException(ErrorCode.E004, "加工单已生成结算单，不可重复结算");
        }
    }

    private void ensureDistinctOrderUuids(List<String> orderUuids) {
        Set<String> seen = new HashSet<>();
        for (String orderUuid : orderUuids) {
            if (!seen.add(orderUuid)) {
                throw new BusinessException(ErrorCode.E004, "加工单重复勾选，不可重复结算");
            }
        }
    }

    private void ensureActiveSettle(SettleOrder settle) {
        if ((settle.getIsDeleted() != null && settle.getIsDeleted() == 1)
                || (settle.getSettleStatus() != null && settle.getSettleStatus() == SETTLE_STATUS_VOID)) {
            throw new BusinessException(ErrorCode.E004, "结算单已作废，不可继续操作");
        }
    }

    private boolean receiveRequestExists(String settleUuid, String requestId) {
        return receiveRecordMapper.selectCount(new LambdaQueryWrapper<ReceiveRecord>()
                .eq(ReceiveRecord::getSettleUuid, settleUuid)
                .eq(ReceiveRecord::getRequestId, requestId)) > 0;
    }

    private void insertReceiveRecord(ReceiveRecord record) {
        try {
            ConcurrencyGuard.requireRowUpdated(receiveRecordMapper.insert(record));
        } catch (DuplicateKeyException e) {
            if (!receiveRequestExists(record.getSettleUuid(), record.getRequestId())) {
                throw e;
            }
        }
    }

    private void insertSettleDetail(SettleDetail detail) {
        try {
            ConcurrencyGuard.requireRowUpdated(settleDetailMapper.insert(detail));
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.E004, "加工单已生成结算单，不可重复结算");
        }
    }

    private void rollbackSettledProcessOrder(String orderUuid) {
        ConcurrencyGuard.requireRowUpdated(processOrderService.getBaseMapper().update(null,
                new LambdaUpdateWrapper<ProcessOrder>()
                        .eq(ProcessOrder::getUuid, orderUuid)
                        .eq(ProcessOrder::getOrderStatus, ORDER_STATUS_SETTLED)
                        .set(ProcessOrder::getOrderStatus, ORDER_STATUS_FINISHED)
                        .set(ProcessOrder::getUpdateTime, LocalDateTime.now())
                        .set(ProcessOrder::getUpdateBy, currentOperator())
                        .setSql("version = version + 1")));
    }

    private void updateSettleSnapshot(SettleOrder settle) {
        ConcurrencyGuard.requireRowUpdated(getBaseMapper().update(null,
                new LambdaUpdateWrapper<SettleOrder>()
                        .eq(SettleOrder::getUuid, settle.getUuid())
                        .eq(SettleOrder::getSettleStatus, SETTLE_STATUS_PENDING)
                        .set(SettleOrder::getSnapBill, settle.getSnapBill())
                        .set(SettleOrder::getSnapBillTime, settle.getSnapBillTime())
                        .set(SettleOrder::getUpdateTime, LocalDateTime.now())
                        .set(SettleOrder::getUpdateBy, currentOperator())
                        .setSql("version = version + 1")));
    }

    private void markSettleVoided(SettleOrder settle, String reason) {
        LocalDateTime voidTime = LocalDateTime.now();
        String voidBy = currentOperator();
        ConcurrencyGuard.requireRowUpdated(getBaseMapper().update(null,
                new LambdaUpdateWrapper<SettleOrder>()
                        .eq(SettleOrder::getUuid, settle.getUuid())
                        .eq(SettleOrder::getSettleStatus, settle.getSettleStatus())
                        .set(SettleOrder::getSettleStatus, SETTLE_STATUS_VOID)
                        .set(SettleOrder::getVoidReason, reason.trim())
                        .set(SettleOrder::getVoidBy, voidBy)
                        .set(SettleOrder::getVoidTime, voidTime)
                        .set(SettleOrder::getUpdateTime, voidTime)
                        .set(SettleOrder::getUpdateBy, voidBy)
                        .setSql("version = version + 1")));
    }

    private String buildSettleSnapshot(SettleOrder settle, List<SettleDetail> details, List<ProcessOrder> orders) {
        List<SettlePrintLineVO> printLines = buildPrintLines(settle, details);
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("schema_version", "1.4");
        snap.put("snapshot_type", "settle_bill");
        snap.put("settle_uuid", settle.getUuid());
        snap.put("settle_no", settle.getSettleNo());
        snap.put("customer_uuid", settle.getCustomerUuid());
        snap.put("customer_name", settle.getCustomerName());
        snap.put("settle_type", settle.getSettleType());
        snap.put("settle_date", settle.getSettleDate());
        snap.put("period_start", settle.getPeriodStart());
        snap.put("period_end", settle.getPeriodEnd());
        snap.put("is_invoice", settle.getIsInvoice());
        snap.put("settle_status", settle.getSettleStatus());
        snap.put("amount_no_tax", settle.getAmountNoTax());
        snap.put("tax_amount", settle.getTaxAmount());
        snap.put("saw_amount", settle.getSawAmount());
        snap.put("rewind_amount", settle.getRewindAmount());
        snap.put("extra_amount", settle.getExtraAmount());
        snap.put("total_amount", settle.getTotalAmount());
        snap.put("received_amount", settle.getReceivedAmount());
        snap.put("cash_received_amount", settle.getCashReceivedAmount());
        snap.put("scrap_offset_amount", settle.getScrapOffsetAmount());
        snap.put("discount_amount", settle.getDiscountAmount());
        snap.put("unreceived_amount", settle.getUnreceivedAmount());
        snap.put("remark", settle.getRemark());
        snap.put("source_orders", buildSettleSnapshotOrders(orders));
        snap.put("detail_items", details);
        snap.put("details", buildSettleSnapshotDetails(details));
        snap.put("print_line_items", printLines);
        snap.put("print_lines", printLines);
        return toJson(snap);
    }

    private List<Map<String, Object>> buildSettleSnapshotOrders(List<ProcessOrder> orders) {
        return orders.stream().map(order -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("order_uuid", order.getUuid());
            row.put("order_no", order.getOrderNo());
            row.put("order_date", order.getOrderDate());
            row.put("customer_name", order.getCustomerName());
            row.put("settle_type", order.getSettleType());
            row.put("is_invoice", order.getIsInvoice());
            row.put("tax_rate", order.getTaxRate());
            row.put("urgent_fee", order.getUrgentFee());
            row.put("pallet_fee", order.getPalletFee());
            row.put("loading_fee", order.getLoadingFee());
            row.put("freight_fee", order.getFreightFee());
            row.put("other_fee", order.getOtherFee());
            row.put("total_original_weight", order.getTotalOriginalWeight());
            row.put("total_finish_weight", order.getTotalFinishWeight());
            row.put("process_amount_no_tax", order.getProcessAmountNoTax());
            row.put("process_amount_tax", order.getProcessAmountTax());
            row.put("extra_amount_no_tax", order.getExtraAmountNoTax());
            row.put("extra_amount_tax", order.getExtraAmountTax());
            row.put("total_process_amount", order.getTotalProcessAmount());
            row.put("total_extra_amount", order.getTotalExtraAmount());
            row.put("total_amount", order.getTotalAmount());
            row.put("remark", order.getRemark());
            return row;
        }).toList();
    }

    private List<Map<String, Object>> buildSettleSnapshotDetails(List<SettleDetail> details) {
        return details.stream().map(detail -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("uuid", detail.getUuid());
            row.put("settle_uuid", detail.getSettleUuid());
            row.put("order_uuid", detail.getOrderUuid());
            row.put("order_no", detail.getOrderNo());
            row.put("saw_amount", detail.getSawAmount());
            row.put("rewind_amount", detail.getRewindAmount());
            row.put("extra_amount", detail.getExtraAmount());
            row.put("order_amount", detail.getOrderAmount());
            row.put("remark", detail.getRemark());
            return row;
        }).toList();
    }

    private List<SettleDetail> readSnapshotDetails(String snapBill) {
        return SettleSnapshotDetailReader.read(snapBill, objectMapper);
    }

    private List<SettlePrintLineVO> readSnapshotPrintLines(String snapBill) {
        return SettleSnapshotPrintLineReader.read(snapBill, objectMapper);
    }

    private SettleOrder snapshotSettleOrder(SettleOrder order) {
        JsonNode root = snapshotRoot(order.getSnapBill());
        if (root == null) {
            return order;
        }
        SettleOrder view = new SettleOrder();
        view.setUuid(order.getUuid());
        view.setSettleNo(textValue(root, "settle_no", "settleNo", order.getSettleNo()));
        view.setCustomerUuid(textValue(root, "customer_uuid", "customerUuid", order.getCustomerUuid()));
        view.setCustomerName(textValue(root, "customer_name", "customerName", order.getCustomerName()));
        view.setSettleType(intValue(root, "settle_type", "settleType", order.getSettleType()));
        view.setSettleDate(dateValue(root, "settle_date", "settleDate", order.getSettleDate()));
        view.setPeriodStart(dateValue(root, "period_start", "periodStart", order.getPeriodStart()));
        view.setPeriodEnd(dateValue(root, "period_end", "periodEnd", order.getPeriodEnd()));
        view.setIsInvoice(intValue(root, "is_invoice", "isInvoice", order.getIsInvoice()));
        view.setSawAmount(decimalValue(root, "saw_amount", "sawAmount", order.getSawAmount()));
        view.setRewindAmount(decimalValue(root, "rewind_amount", "rewindAmount", order.getRewindAmount()));
        view.setExtraAmount(decimalValue(root, "extra_amount", "extraAmount", order.getExtraAmount()));
        view.setAmountNoTax(decimalValue(root, "amount_no_tax", "amountNoTax", order.getAmountNoTax()));
        view.setTaxAmount(decimalValue(root, "tax_amount", "taxAmount", order.getTaxAmount()));
        view.setTotalAmount(decimalValue(root, "total_amount", "totalAmount", order.getTotalAmount()));
        view.setReceivedAmount(order.getReceivedAmount());
        view.setCashReceivedAmount(decimalValue(root, "cash_received_amount", "cashReceivedAmount", order.getCashReceivedAmount()));
        view.setScrapOffsetAmount(decimalValue(root, "scrap_offset_amount", "scrapOffsetAmount", order.getScrapOffsetAmount()));
        view.setDiscountAmount(decimalValue(root, "discount_amount", "discountAmount", order.getDiscountAmount()));
        view.setUnreceivedAmount(order.getUnreceivedAmount());
        view.setSettleStatus(order.getSettleStatus());
        view.setSnapBill(order.getSnapBill());
        view.setSnapBillTime(order.getSnapBillTime());
        view.setRemark(textValue(root, "remark", "remark", order.getRemark()));
        return view;
    }

    private JsonNode snapshotRoot(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            log.warn("结算快照解析失败，将按当前业务数据兜底展示：{}", ex.getMessage());
            return null;
        }
    }

    private JsonNode firstExisting(JsonNode root, String... names) {
        for (String name : names) {
            JsonNode node = root.get(name);
            if (node != null && !node.isNull()) {
                return node;
            }
        }
        return null;
    }

    private JsonNode field(JsonNode root, String snakeName, String camelName) {
        JsonNode node = root.get(snakeName);
        if (node == null || node.isNull()) {
            node = root.get(camelName);
        }
        return node == null || node.isNull() ? null : node;
    }

    private String textValue(JsonNode root, String snakeName, String camelName, String fallback) {
        JsonNode node = field(root, snakeName, camelName);
        return node == null ? fallback : node.asText();
    }

    private Integer intValue(JsonNode root, String snakeName, String camelName, Integer fallback) {
        JsonNode node = field(root, snakeName, camelName);
        if (node == null) {
            return fallback;
        }
        try {
            return node.isNumber() ? node.asInt() : Integer.parseInt(node.asText());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private BigDecimal decimalValue(JsonNode root, String snakeName, String camelName, BigDecimal fallback) {
        JsonNode node = field(root, snakeName, camelName);
        if (node == null) {
            return fallback;
        }
        try {
            return node.isNumber() ? node.decimalValue() : new BigDecimal(node.asText());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private LocalDate dateValue(JsonNode root, String snakeName, String camelName, LocalDate fallback) {
        JsonNode node = field(root, snakeName, camelName);
        if (node == null) {
            return fallback;
        }
        try {
            if (node.isArray() && node.size() >= 3) {
                return LocalDate.of(node.get(0).asInt(), node.get(1).asInt(), node.get(2).asInt());
            }
            String text = node.asText();
            return StringUtils.hasText(text) ? LocalDate.parse(text.substring(0, Math.min(10, text.length()))) : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException("结算快照生成失败");
        }
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
        return resolveInvoice(context.orders(), context.isInvoice(), customer);
    }

    private SettlementAmounts sumAmounts(List<ProcessOrder> orders, Integer isInvoice, Customer customer) {
        SettlementAmountCalculator.Calculation amount =
                settlementAmountCalculator.calculate(orders, isInvoice, customer);
        if (amount.pendingPriceCount() > 0) {
            throw new BusinessException("存在待核价加工单，请完成核价后再生成结算单");
        }
        return new SettlementAmounts(amount.saw(), amount.rewind(), amount.extra(), amount.noTax(),
                amount.tax(), amount.total(), amount.details());
    }

    private SettleQuoteVO quote(List<ProcessOrder> orders, Integer requestedInvoice) {
        Customer customer = resolveSingleCustomer(orders);
        Integer isInvoice = resolveInvoice(orders, requestedInvoice, customer);
        SettlementAmountCalculator.Calculation amount =
                settlementAmountCalculator.calculate(orders, isInvoice, customer);
        return new SettleQuoteVO(orders.size(), amount.pendingPriceCount(), isInvoice,
                amount.saw(), amount.rewind(), amount.extra(), amount.noTax(), amount.tax(), amount.total());
    }

    private Integer resolveInvoice(List<ProcessOrder> orders, Integer requestedInvoice, Customer customer) {
        if (requestedInvoice != null) return requestedInvoice;
        Set<Integer> invoiceValues = orders.stream().map(ProcessOrder::getIsInvoice)
                .filter(value -> value != null).collect(Collectors.toSet());
        if (invoiceValues.size() > 1) {
            throw new BusinessException("所选加工单开票状态不一致，请明确选择开票状态");
        }
        if (invoiceValues.size() == 1) return invoiceValues.iterator().next();
        return customer.getDefaultInvoice() == null ? 2 : customer.getDefaultInvoice();
    }

    private List<ProcessOrder> findMonthlyOrders(String customerUuid, LocalDate start, LocalDate end) {
        return processOrderService.list(new LambdaQueryWrapper<ProcessOrder>()
                .eq(ProcessOrder::getCustomerUuid, customerUuid)
                .eq(ProcessOrder::getOrderStatus, ORDER_STATUS_FINISHED)
                .ge(ProcessOrder::getOrderDate, start)
                .le(ProcessOrder::getOrderDate, end)
                .orderByAsc(ProcessOrder::getOrderDate)
                .orderByAsc(ProcessOrder::getOrderNo));
    }

    private BigDecimal settleOrderAmount(ProcessOrder order, BigDecimal fallbackAmount) {
        if (order != null && order.getTotalAmount() != null) {
            return money(order.getTotalAmount());
        }
        return money(fallbackAmount);
    }

    private BigDecimal detailBaseAmount(SettleDetail detail) {
        return nz(detail.getSawAmount()).add(nz(detail.getRewindAmount())).add(nz(detail.getExtraAmount()))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal invoiceIncrease(BigDecimal noTaxAmount, Integer isInvoice, BigDecimal taxRate) {
        return FeeCalculator.tax(noTaxAmount, taxRate, isInvoice != null && isInvoice == 1);
    }

    private BigDecimal invoiceTotal(BigDecimal noTaxAmount, Integer isInvoice, BigDecimal taxRate) {
        return noTaxAmount.add(invoiceIncrease(noTaxAmount, isInvoice, taxRate))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal invoiceUnitPrice(BigDecimal unitPrice, Integer isInvoice, BigDecimal taxRate) {
        if (unitPrice == null) {
            return null;
        }
        return invoiceTotal(unitPrice, isInvoice, taxRate);
    }

    private BigDecimal taxRateOf(ProcessOrder order, Customer customer) {
        if (order != null && order.getTaxRate() != null) {
            return order.getTaxRate();
        }
        return customer == null ? BigDecimal.ZERO : nz(customer.getTaxRate());
    }

    private void validateFinishedOrder(ProcessOrder order) {
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        if (order.getOrderStatus() == null || order.getOrderStatus() != ORDER_STATUS_FINISHED) {
            throw new BusinessException("加工单非已完成状态，不可结算：" + order.getOrderNo());
        }
    }

    /** 生成结算单号：由系统单号规则配置生成，唯一索引兜底防并发重复。 */
    private String nextSettleNo(LocalDate date) {
        return documentNoService.next(NoRuleBizType.SETTLE_ORDER, date);
    }

    private String resolveOperator(String operator) {
        if (operator != null && !operator.isBlank()) {
            return operator;
        }
        return currentOperator();
    }

    private String currentOperator() {
        return AuthContextHolder.currentDisplayName();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static BigDecimal money(BigDecimal v) {
        return nz(v).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
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

    private record LineAmounts(
            BigDecimal sawWeight,
            BigDecimal rewindWeight,
            BigDecimal sawUnitPrice,
            BigDecimal rewindUnitPrice,
            BigDecimal sawAmount,
            BigDecimal rewindAmount) {

        BigDecimal processAmount() {
            return sawAmount.add(rewindAmount);
        }
    }
}
