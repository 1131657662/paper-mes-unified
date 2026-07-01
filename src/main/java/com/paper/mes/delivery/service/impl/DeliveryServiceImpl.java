package com.paper.mes.delivery.service.impl;

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
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.delivery.dto.AvailableFinishVO;
import com.paper.mes.delivery.dto.DeliveryAppendItemsDTO;
import com.paper.mes.delivery.dto.DeliveryConfirmDTO;
import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.delivery.dto.DeliveryDetailItemVO;
import com.paper.mes.delivery.dto.DeliveryDetailVO;
import com.paper.mes.delivery.dto.DeliveryQuery;
import com.paper.mes.delivery.dto.DeliveryRollbackDTO;
import com.paper.mes.delivery.dto.DeliveryRollbackSnapshotVO;
import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.delivery.mapper.DeliveryOrderMapper;
import com.paper.mes.delivery.service.DeliveryExportService;
import com.paper.mes.delivery.service.DeliveryService;
import com.paper.mes.machine.entity.Machine;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.oplog.entity.OperationLog;
import com.paper.mes.oplog.mapper.OperationLogMapper;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import com.paper.mes.settle.entity.SettleDetail;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import com.paper.mes.system.config.constant.NoRuleBizType;
import com.paper.mes.system.config.service.DocumentNoService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl extends ServiceImpl<DeliveryOrderMapper, DeliveryOrder>
        implements DeliveryService {

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
    private final FinishOriginalRelMapper finishOriginalRelMapper;
    private final OriginalRollMapper originalRollMapper;
    private final ProcessOrderMapper processOrderMapper;
    private final ProcessStepMapper processStepMapper;
    private final SettleDetailMapper settleDetailMapper;
    private final SettleOrderMapper settleOrderMapper;
    private final MachineMapper machineMapper;
    private final CustomerService customerService;
    private final DeliveryExportService deliveryExportService;
    private final OperationLogMapper operationLogMapper;
    private final OperationLogService operationLogService;
    private final DocumentNoService documentNoService;
    private final BusinessLockService businessLockService;
    private final ObjectMapper objectMapper;

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
        if (query.getDateFrom() != null) {
            wrapper.ge(DeliveryOrder::getDeliveryDate, query.getDateFrom());
        }
        if (query.getDateTo() != null) {
            wrapper.le(DeliveryOrder::getDeliveryDate, query.getDateTo());
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
        businessLockService.lockFinishRolls(dto.getItems().stream()
                .map(DeliveryCreateDTO.Item::getFinishUuid)
                .toList());
        Customer customer = customerService.getById(dto.getCustomerUuid());
        if (customer == null) {
            throw new BusinessException(ErrorCode.E002, "客户不存在");
        }

        // 逐件校验成品：存在、已入库(2)、归属当前客户。
        List<FinishRoll> picked = new ArrayList<>(dto.getItems().size());
        Map<String, String> orderNoCache = new LinkedHashMap<>();
        List<String> lockedFinishUuids = pendingDeliveryFinishUuids();
        Set<String> requestFinishUuids = new HashSet<>();
        boolean hasCashOrder = false;
        for (DeliveryCreateDTO.Item item : dto.getItems()) {
            if (!requestFinishUuids.add(item.getFinishUuid())) {
                throw new BusinessException("出库成品重复：" + item.getFinishUuid());
            }
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
            validateOutWeight(f, outWeight);
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
        ConcurrencyGuard.requireUpdated(save(deliveryOrder));

        for (DeliveryDetail d : details) {
            d.setDeliveryUuid(deliveryOrder.getUuid());
            insertDeliveryDetail(d);
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
        List<DeliveryDetail> details = deliveryDetails(uuid);
        DeliveryDetailVO vo = new DeliveryDetailVO();
        vo.setOrder(snapshotDeliveryOrder(order));
        List<DeliveryDetailItemVO> snapshotItems = readSnapshotDeliveryItems(order.getSnapDelivery());
        vo.setDetails(snapshotItems == null ? buildDetailItems(details) : snapshotItems);
        vo.setRollbackSnapshot(readRollbackSnapshot(order.getSnapDelivery()));
        vo.setOperationLogs(loadOperationLogs(order));
        return vo;
    }

    @Override
    public void exportDetail(String uuid, HttpServletResponse response) {
        DeliveryDetailVO detail = getDetail(uuid);
        String filename = "出库单_" + detail.getOrder().getDeliveryNo() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(filename));
        try (Workbook workbook = deliveryExportService.buildWorkbook(detail)) {
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            throw new BusinessException("导出出库单失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(String uuid, DeliveryConfirmDTO dto) {
        businessLockService.lockDeliveryOrder(uuid);
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
        if (details.isEmpty()) {
            throw new BusinessException("出库单没有明细，不可确认");
        }
        businessLockService.lockFinishRolls(details.stream().map(DeliveryDetail::getFinishUuid).toList());
        // 逐件扣库存：成品须仍为已入库(2)，置为已出库(3)。
        for (DeliveryDetail d : details) {
            FinishRoll f = finishRollMapper.selectById(d.getFinishUuid());
            if (f == null || f.getFinishStatus() == null
                    || f.getFinishStatus() != FINISH_STATUS_IN_STOCK) {
                throw new BusinessException("成品状态已变更，不可出库：" + d.getFinishRollNo());
            }
            updateFinishStatus(d.getFinishUuid(), FINISH_STATUS_IN_STOCK, FINISH_STATUS_OUT);
        }

        order.setDeliveryStatus(DELIVERY_STATUS_OUT);
        order.setSignUser(dto == null ? null : dto.getSignUser());
        order.setSignTime(dto != null && dto.getSignTime() != null ? dto.getSignTime() : LocalDateTime.now());
        if (dto != null && StringUtils.hasText(dto.getRemark())) {
            order.setRemark(dto.getRemark());
        }
        order.setSnapDelivery(buildDeliverySnapshot(order, details));
        order.setSnapDeliveryTime(LocalDateTime.now());
        updateDeliveryForConfirm(order);

        operationLogService.record(OperationLogService.BIZ_TYPE_DELIVERY,
                order.getUuid(), order.getDeliveryNo(),
                OperationLogService.ACTION_DELIVERY_CONFIRM,
                dto == null ? null : dto.getSignUser(), "出库确认签收");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollback(String uuid, DeliveryRollbackDTO dto) {
        businessLockService.lockDeliveryOrder(uuid);
        DeliveryOrder order = requireOrder(uuid);
        if (order.getDeliveryStatus() == null || order.getDeliveryStatus() != DELIVERY_STATUS_OUT) {
            throw new BusinessException("出库单非已出库状态，不可回退");
        }
        List<DeliveryDetail> details = deliveryDetails(uuid);
        businessLockService.lockProcessOrders(details.stream().map(DeliveryDetail::getOrderUuid).toList());
        businessLockService.lockFinishRolls(details.stream().map(DeliveryDetail::getFinishUuid).toList());
        ensureOrdersNotSettled(details);
        for (DeliveryDetail detail : details) {
            FinishRoll finish = finishRollMapper.selectById(detail.getFinishUuid());
            if (finish == null || finish.getFinishStatus() == null
                    || finish.getFinishStatus() != FINISH_STATUS_OUT) {
                throw new BusinessException("成品状态已变更，不可回退：" + detail.getFinishRollNo());
            }
            updateFinishStatus(detail.getFinishUuid(), FINISH_STATUS_OUT, FINISH_STATUS_IN_STOCK);
        }
        String rollbackReason = dto.getReason().trim();
        String rollbackOperator = currentOperator();
        LocalDateTime rollbackTime = LocalDateTime.now();
        String rollbackSnapshot = buildRollbackSnapshot(order, rollbackReason, rollbackOperator, rollbackTime);
        order.setDeliveryStatus(DELIVERY_STATUS_PENDING);
        order.setSignUser(null);
        order.setSignTime(null);
        order.setSnapDelivery(rollbackSnapshot);
        order.setSnapDeliveryTime(rollbackTime);
        order.setRemark(appendRemark(order.getRemark(), "回退出库：" + rollbackReason));
        updateDeliveryForRollback(order);
        operationLogService.record(OperationLogService.BIZ_TYPE_DELIVERY,
                order.getUuid(), order.getDeliveryNo(),
                OperationLogService.ACTION_ROLLBACK, null,
                "回退出库签收：" + rollbackReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void appendDetails(String uuid, DeliveryAppendItemsDTO dto) {
        businessLockService.lockDeliveryOrder(uuid);
        businessLockService.lockFinishRolls(dto.getItems().stream()
                .map(DeliveryAppendItemsDTO.Item::getFinishUuid)
                .toList());
        DeliveryOrder order = requireOrder(uuid);
        if (order.getDeliveryStatus() == null || order.getDeliveryStatus() != DELIVERY_STATUS_PENDING) {
            throw new BusinessException("仅待出库单允许追加明细");
        }

        List<DeliveryDetail> existingDetails = deliveryDetails(uuid);
        Set<String> existingFinishUuids = existingDetails.stream()
                .map(DeliveryDetail::getFinishUuid)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Set<String> requestFinishUuids = new HashSet<>();
        List<String> lockedFinishUuids = pendingDeliveryFinishUuids();
        boolean hasCashOrder = false;
        List<DeliveryDetail> appendDetails = new ArrayList<>(dto.getItems().size());

        for (DeliveryAppendItemsDTO.Item item : dto.getItems()) {
            if (!requestFinishUuids.add(item.getFinishUuid())) {
                throw new BusinessException("追加成品重复：" + item.getFinishUuid());
            }
            if (existingFinishUuids.contains(item.getFinishUuid())) {
                throw new BusinessException("成品已在本张出库单中：" + item.getFinishUuid());
            }
            if (lockedFinishUuids.contains(item.getFinishUuid())) {
                throw new BusinessException("成品已在其他待出库单中：" + item.getFinishUuid());
            }
            FinishRoll finish = finishRollMapper.selectById(item.getFinishUuid());
            if (finish == null) {
                throw new BusinessException(ErrorCode.E002, "成品不存在：" + item.getFinishUuid());
            }
            if (finish.getFinishStatus() == null || finish.getFinishStatus() != FINISH_STATUS_IN_STOCK) {
                throw new BusinessException("成品非已入库状态，不可追加出库：" + finish.getFinishRollNo());
            }
            ProcessOrder processOrder = processOrderMapper.selectById(finish.getOrderUuid());
            if (processOrder == null || !order.getCustomerUuid().equals(processOrder.getCustomerUuid())) {
                throw new BusinessException("成品不属于该出库单客户：" + finish.getFinishRollNo());
            }
            if (processOrder.getSettleType() != null && processOrder.getSettleType() == SETTLE_TYPE_CASH) {
                hasCashOrder = true;
            }
            appendDetails.add(buildDeliveryDetail(finish, item));
        }

        if (hasCashOrder && hasUnsettledCash(order.getCustomerUuid())) {
            if (!dto.isForceRelease()) {
                throw new BusinessException("次结加工单存在未结清款项，禁止追加出库");
            }
            order.setSettleBlockAction(BLOCK_RELEASE);
            operationLogService.record(OperationLogService.BIZ_TYPE_DELIVERY,
                    order.getUuid(), order.getDeliveryNo(),
                    OperationLogService.ACTION_DELIVERY_RELEASE, null,
                    "待出库改单追加成品时警告放行");
        }

        for (DeliveryDetail detail : appendDetails) {
            detail.setDeliveryUuid(order.getUuid());
            insertDeliveryDetail(detail);
        }
        refreshTotals(order);
        operationLogService.record(OperationLogService.BIZ_TYPE_DELIVERY,
                order.getUuid(), order.getDeliveryNo(),
                OperationLogService.ACTION_FIELD_MODIFY, null,
                "出库改单追加明细：" + appendDetails.size() + " 卷");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeDetail(String uuid, String detailUuid) {
        businessLockService.lockDeliveryOrder(uuid);
        DeliveryOrder order = requireOrder(uuid);
        if (order.getDeliveryStatus() == null || order.getDeliveryStatus() != DELIVERY_STATUS_PENDING) {
            throw new BusinessException("仅待出库单允许移出明细");
        }
        DeliveryDetail detail = deliveryDetailMapper.selectById(detailUuid);
        if (detail == null || !uuid.equals(detail.getDeliveryUuid())) {
            throw new BusinessException(ErrorCode.E002, "出库明细不存在");
        }
        ConcurrencyGuard.requireRowUpdated(deliveryDetailMapper.delete(new LambdaQueryWrapper<DeliveryDetail>()
                .eq(DeliveryDetail::getUuid, detailUuid)
                .eq(DeliveryDetail::getDeliveryUuid, uuid)));
        refreshTotals(order);
        operationLogService.record(OperationLogService.BIZ_TYPE_DELIVERY,
                order.getUuid(), order.getDeliveryNo(),
                OperationLogService.ACTION_FIELD_MODIFY, null,
                "出库改单移出明细：" + detail.getFinishRollNo());
    }

    /** 生成出库单号：由系统单号规则配置生成，唯一索引兜底防并发重复。 */
    private String nextDeliveryNo(LocalDate date) {
        return documentNoService.next(NoRuleBizType.DELIVERY_ORDER, date);
    }

    private DeliveryOrder requireOrder(String uuid) {
        DeliveryOrder order = getById(uuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "出库单不存在");
        }
        return order;
    }

    private String buildDeliverySnapshot(DeliveryOrder order, List<DeliveryDetail> details) {
        List<DeliveryDetailItemVO> detailItems = buildDetailItems(details);
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("schema_version", "1.1");
        snap.put("snapshot_type", "delivery_confirm");
        snap.put("delivery_uuid", order.getUuid());
        snap.put("delivery_no", order.getDeliveryNo());
        snap.put("customer_uuid", order.getCustomerUuid());
        snap.put("customer_name", order.getCustomerName());
        snap.put("delivery_date", order.getDeliveryDate());
        snap.put("delivery_status", order.getDeliveryStatus());
        snap.put("picker_name", order.getPickerName());
        snap.put("car_no", order.getCarNo());
        snap.put("container_no", order.getContainerNo());
        snap.put("sign_user", order.getSignUser());
        snap.put("sign_time", order.getSignTime());
        snap.put("settle_block_action", order.getSettleBlockAction());
        snap.put("total_count", order.getTotalCount());
        snap.put("total_weight", order.getTotalWeight());
        snap.put("remark", order.getRemark());
        snap.put("detail_items", detailItems);
        snap.put("details", buildDeliverySnapshotItems(detailItems));
        return toJson(snap);
    }

    private String buildRollbackSnapshot(DeliveryOrder order, String reason, String operator, LocalDateTime rollbackTime) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("schema_version", "1.1");
        snap.put("snapshot_type", "delivery_rollback");
        snap.put("delivery_uuid", order.getUuid());
        snap.put("delivery_no", order.getDeliveryNo());
        snap.put("customer_uuid", order.getCustomerUuid());
        snap.put("customer_name", order.getCustomerName());
        snap.put("delivery_status_before", order.getDeliveryStatus());
        snap.put("delivery_status_after", DELIVERY_STATUS_PENDING);
        snap.put("rollback_reason", reason);
        snap.put("rollback_operator", operator);
        snap.put("rollback_time", rollbackTime);
        snap.put("previous_confirm_snapshot", snapshotAsObject(order.getSnapDelivery()));
        return toJson(snap);
    }

    private List<Map<String, Object>> buildDeliverySnapshotItems(List<DeliveryDetailItemVO> items) {
        return items.stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("uuid", item.getUuid());
            row.put("delivery_uuid", item.getDeliveryUuid());
            row.put("finish_uuid", item.getFinishUuid());
            row.put("finish_roll_no", item.getFinishRollNo());
            row.put("order_uuid", item.getOrderUuid());
            row.put("order_no", item.getOrderNo());
            row.put("paper_name", item.getPaperName());
            row.put("gram_weight", item.getGramWeight());
            row.put("finish_width", item.getFinishWidth());
            row.put("finish_diameter", item.getFinishDiameter());
            row.put("finish_core_diameter", item.getFinishCoreDiameter());
            row.put("actual_weight", item.getActualWeight());
            row.put("out_weight", item.getOutWeight());
            row.put("source_type", item.getSourceType());
            row.put("finish_status", item.getFinishStatus());
            row.put("original_roll_nos", item.getOriginalRollNos());
            row.put("original_summary", item.getOriginalSummary());
            row.put("process_mode_text", item.getProcessModeText());
            row.put("process_summary", item.getProcessSummary());
            List<DeliveryDetailItemVO.OriginalSourceItem> originalItems =
                    item.getOriginalItems() == null ? List.of() : item.getOriginalItems();
            row.put("original_items", originalItems);
            row.put("process_step_items", item.getProcessStepItems());
            row.put("machine_names", originalItems.stream()
                    .map(DeliveryDetailItemVO.OriginalSourceItem::getMachineName)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toList());
            row.put("remark", item.getRemark());
            row.put("finish_remark", item.getFinishRemark());
            row.put("actual_remark", item.getActualRemark());
            return row;
        }).toList();
    }

    private List<DeliveryDetailItemVO> readSnapshotDeliveryItems(String snapDelivery) {
        if (isRollbackSnapshot(snapDelivery)) {
            return null;
        }
        return DeliverySnapshotItemReader.read(snapDelivery, objectMapper);
    }

    private DeliveryRollbackSnapshotVO readRollbackSnapshot(String snapDelivery) {
        return DeliveryRollbackSnapshotReader.read(snapDelivery, objectMapper);
    }

    private DeliveryOrder snapshotDeliveryOrder(DeliveryOrder order) {
        DeliveryOrder view = copyDeliveryOrder(order);
        JsonNode root = snapshotRoot(order.getSnapDelivery());
        if (root == null) {
            return view;
        }
        if (isSnapshotType(root, "delivery_rollback")) {
            return view;
        }
        view.setDeliveryNo(textValue(root, "delivery_no", "deliveryNo", order.getDeliveryNo()));
        view.setCustomerUuid(textValue(root, "customer_uuid", "customerUuid", order.getCustomerUuid()));
        view.setCustomerName(textValue(root, "customer_name", "customerName", order.getCustomerName()));
        view.setDeliveryDate(dateValue(root, "delivery_date", "deliveryDate", order.getDeliveryDate()));
        view.setDeliveryStatus(intValue(root, "delivery_status", "deliveryStatus", order.getDeliveryStatus()));
        view.setPickerName(textValue(root, "picker_name", "pickerName", order.getPickerName()));
        view.setCarNo(textValue(root, "car_no", "carNo", order.getCarNo()));
        view.setContainerNo(textValue(root, "container_no", "containerNo", order.getContainerNo()));
        view.setSignUser(textValue(root, "sign_user", "signUser", order.getSignUser()));
        view.setSignTime(dateTimeValue(root, "sign_time", "signTime", order.getSignTime()));
        view.setSettleBlockAction(intValue(root, "settle_block_action", "settleBlockAction", order.getSettleBlockAction()));
        view.setTotalCount(intValue(root, "total_count", "totalCount", order.getTotalCount()));
        view.setTotalWeight(decimalValue(root, "total_weight", "totalWeight", order.getTotalWeight()));
        view.setRemark(textValue(root, "remark", "remark", order.getRemark()));
        return view;
    }

    private DeliveryOrder copyDeliveryOrder(DeliveryOrder order) {
        DeliveryOrder view = new DeliveryOrder();
        view.setUuid(order.getUuid());
        view.setDeliveryNo(order.getDeliveryNo());
        view.setCustomerUuid(order.getCustomerUuid());
        view.setCustomerName(order.getCustomerName());
        view.setDeliveryDate(order.getDeliveryDate());
        view.setTotalCount(order.getTotalCount());
        view.setTotalWeight(order.getTotalWeight());
        view.setPickerName(order.getPickerName());
        view.setCarNo(order.getCarNo());
        view.setContainerNo(order.getContainerNo());
        view.setSignUser(order.getSignUser());
        view.setSignTime(order.getSignTime());
        view.setSettleBlockAction(order.getSettleBlockAction());
        view.setDeliveryStatus(order.getDeliveryStatus());
        view.setSnapDelivery(order.getSnapDelivery());
        view.setSnapDeliveryTime(order.getSnapDeliveryTime());
        view.setRemark(order.getRemark());
        view.setCreateTime(order.getCreateTime());
        view.setUpdateTime(order.getUpdateTime());
        return view;
    }

    private JsonNode snapshotRoot(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isRollbackSnapshot(String json) {
        JsonNode root = snapshotRoot(json);
        return root != null && isSnapshotType(root, "delivery_rollback");
    }

    private boolean isSnapshotType(JsonNode root, String type) {
        JsonNode node = root.get("snapshot_type");
        if (node == null || node.isNull()) {
            node = root.get("snapshotType");
        }
        return node != null && type.equals(node.asText());
    }

    private Object snapshotAsObject(String json) {
        JsonNode root = snapshotRoot(json);
        if (root == null) {
            return null;
        }
        return objectMapper.convertValue(root, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
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

    private LocalDateTime dateTimeValue(JsonNode root, String snakeName, String camelName, LocalDateTime fallback) {
        JsonNode node = field(root, snakeName, camelName);
        if (node == null) {
            return fallback;
        }
        try {
            if (node.isArray() && node.size() >= 5) {
                int second = node.size() >= 6 ? node.get(5).asInt() : 0;
                return LocalDateTime.of(node.get(0).asInt(), node.get(1).asInt(), node.get(2).asInt(),
                        node.get(3).asInt(), node.get(4).asInt(), second);
            }
            String text = node.asText();
            if (!StringUtils.hasText(text)) {
                return fallback;
            }
            text = text.replace(' ', 'T');
            if (text.length() == 10) {
                return LocalDate.parse(text).atStartOfDay();
            }
            return LocalDateTime.parse(text.substring(0, Math.min(19, text.length())));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException("出库快照生成失败");
        }
    }

    private List<DeliveryDetail> deliveryDetails(String uuid) {
        return deliveryDetailMapper.selectList(new LambdaQueryWrapper<DeliveryDetail>()
                .eq(DeliveryDetail::getDeliveryUuid, uuid)
                .orderByAsc(DeliveryDetail::getCreateTime));
    }

    private List<DeliveryDetailItemVO> buildDetailItems(List<DeliveryDetail> details) {
        if (details.isEmpty()) {
            return List.of();
        }
        Map<String, FinishRoll> finishByUuid = loadFinishByUuid(details);
        Map<String, ProcessOrder> orderByUuid = loadOrderByUuid(details);
        Map<String, SourceTrace> traceByFinishUuid = loadTraceByFinishUuid(details);
        List<DeliveryDetailItemVO> result = new ArrayList<>(details.size());
        for (DeliveryDetail detail : details) {
            result.add(toDetailItem(detail, finishByUuid.get(detail.getFinishUuid()),
                    orderByUuid.get(detail.getOrderUuid()), traceByFinishUuid.get(detail.getFinishUuid())));
        }
        return result;
    }

    private DeliveryDetail buildDeliveryDetail(FinishRoll finish, DeliveryAppendItemsDTO.Item item) {
        BigDecimal outWeight = item.getOutWeight() != null ? item.getOutWeight()
                : (finish.getActualWeight() != null ? finish.getActualWeight() : BigDecimal.ZERO);
        validateOutWeight(finish, outWeight);
        DeliveryDetail detail = new DeliveryDetail();
        detail.setFinishUuid(finish.getUuid());
        detail.setOrderUuid(finish.getOrderUuid());
        detail.setFinishRollNo(finish.getFinishRollNo());
        detail.setPaperName(finish.getPaperName());
        detail.setOutWeight(outWeight);
        detail.setRemark(item.getRemark());
        return detail;
    }

    private void insertDeliveryDetail(DeliveryDetail detail) {
        try {
            ConcurrencyGuard.requireRowUpdated(deliveryDetailMapper.insert(detail));
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.E004, "成品已被出库单占用，不可重复出库：" + detail.getFinishRollNo());
        }
    }

    private void validateOutWeight(FinishRoll finish, BigDecimal outWeight) {
        if (outWeight == null || outWeight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("出库重量必须大于 0：" + finish.getFinishRollNo());
        }
        BigDecimal actualWeight = finish.getActualWeight();
        if (actualWeight != null && actualWeight.compareTo(BigDecimal.ZERO) > 0
                && outWeight.compareTo(actualWeight) > 0) {
            throw new BusinessException("出库重量不能大于件重：" + finish.getFinishRollNo());
        }
    }

    private Map<String, FinishRoll> loadFinishByUuid(List<DeliveryDetail> details) {
        List<String> finishUuids = details.stream()
                .map(DeliveryDetail::getFinishUuid)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (finishUuids.isEmpty()) {
            return Map.of();
        }
        return finishRollMapper.selectBatchIds(finishUuids).stream()
                .collect(Collectors.toMap(FinishRoll::getUuid, Function.identity()));
    }

    private Map<String, ProcessOrder> loadOrderByUuid(List<DeliveryDetail> details) {
        List<String> orderUuids = details.stream()
                .map(DeliveryDetail::getOrderUuid)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (orderUuids.isEmpty()) {
            return Map.of();
        }
        return processOrderMapper.selectBatchIds(orderUuids).stream()
                .collect(Collectors.toMap(ProcessOrder::getUuid, Function.identity()));
    }

    private Map<String, SourceTrace> loadTraceByFinishUuid(List<DeliveryDetail> details) {
        List<String> finishUuids = details.stream()
                .map(DeliveryDetail::getFinishUuid)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (finishUuids.isEmpty()) {
            return Map.of();
        }
        Map<String, FinishRoll> finishByUuid = finishRollMapper.selectBatchIds(finishUuids).stream()
                .collect(Collectors.toMap(FinishRoll::getUuid, Function.identity()));
        List<String> orderUuids = details.stream()
                .map(detail -> resolveOrderUuid(detail, finishByUuid.get(detail.getFinishUuid())))
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        Map<String, List<OriginalRoll>> originalsByOrder = loadOriginalsByOrder(orderUuids);
        Map<String, OriginalRoll> originalByUuid = originalsByOrder.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toMap(OriginalRoll::getUuid, Function.identity(), (left, right) -> left));
        Map<String, String> machineNameByUuid = loadMachineNames(originalByUuid.values().stream().toList());
        Map<String, List<ProcessStep>> stepsByOriginal = loadStepsByOriginal(originalByUuid.keySet().stream().toList());
        List<FinishOriginalRel> rels = finishOriginalRelMapper.selectList(new LambdaQueryWrapper<FinishOriginalRel>()
                .in(FinishOriginalRel::getFinishUuid, finishUuids));
        Map<String, List<OriginalRoll>> originalsByFinish = new LinkedHashMap<>();
        for (FinishOriginalRel rel : rels) {
            OriginalRoll original = originalByUuid.get(rel.getOriginalUuid());
            if (original == null) {
                continue;
            }
            originalsByFinish.computeIfAbsent(rel.getFinishUuid(), key -> new ArrayList<>()).add(original);
        }
        Map<String, SourceTrace> result = new LinkedHashMap<>();
        for (DeliveryDetail detail : details) {
            FinishRoll finish = finishByUuid.get(detail.getFinishUuid());
            String orderUuid = resolveOrderUuid(detail, finish);
            List<OriginalRoll> originals = originalsByFinish.getOrDefault(detail.getFinishUuid(), List.of());
            boolean inferred = originals.isEmpty();
            if (inferred) {
                originals = inferOriginals(finish, originalsByOrder.getOrDefault(orderUuid, List.of()));
            }
            result.put(detail.getFinishUuid(), buildSourceTrace(detail, finish, originals, stepsByOriginal,
                    machineNameByUuid, inferred));
        }
        return result;
    }

    private Map<String, OriginalRoll> loadOriginalByUuid(List<FinishOriginalRel> rels) {
        List<String> originalUuids = rels.stream()
                .map(FinishOriginalRel::getOriginalUuid)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (originalUuids.isEmpty()) {
            return Map.of();
        }
        return originalRollMapper.selectBatchIds(originalUuids).stream()
                .collect(Collectors.toMap(OriginalRoll::getUuid, Function.identity()));
    }

    private Map<String, List<OriginalRoll>> loadOriginalsByOrder(List<String> orderUuids) {
        if (orderUuids.isEmpty()) {
            return Map.of();
        }
        List<OriginalRoll> originals = originalRollMapper.selectList(new LambdaQueryWrapper<OriginalRoll>()
                .in(OriginalRoll::getOrderUuid, orderUuids)
                .orderByAsc(OriginalRoll::getOrderUuid)
                .orderByAsc(OriginalRoll::getRowSort));
        return originals.stream().collect(Collectors.groupingBy(OriginalRoll::getOrderUuid,
                LinkedHashMap::new, Collectors.toList()));
    }

    private Map<String, List<ProcessStep>> loadStepsByOriginal(List<String> originalUuids) {
        if (originalUuids.isEmpty()) {
            return Map.of();
        }
        List<ProcessStep> steps = processStepMapper.selectList(new LambdaQueryWrapper<ProcessStep>()
                .in(ProcessStep::getOriginalUuid, originalUuids)
                .orderByAsc(ProcessStep::getStepSort));
        return steps.stream().collect(Collectors.groupingBy(ProcessStep::getOriginalUuid,
                LinkedHashMap::new, Collectors.toList()));
    }

    private Map<String, String> loadMachineNames(List<OriginalRoll> originals) {
        List<String> machineUuids = originals.stream()
                .map(OriginalRoll::getMachineUuid)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (machineUuids.isEmpty()) {
            return Map.of();
        }
        return machineMapper.selectBatchIds(machineUuids).stream()
                .collect(Collectors.toMap(Machine::getUuid, machine -> text(machine.getMachineName()),
                        (left, right) -> left));
    }

    private SourceTrace buildSourceTrace(DeliveryDetail detail, FinishRoll finish, List<OriginalRoll> originals,
                                         Map<String, List<ProcessStep>> stepsByOriginal,
                                         Map<String, String> machineNameByUuid, boolean inferred) {
        if (originals.isEmpty()) {
            return new SourceTrace(unlinkedSourceText(detail, finish), sourceTypeText(finish),
                    "来源关系未建立，无法定位母卷加工方案", List.of(), List.of());
        }
        List<ProcessStep> steps = originals.stream()
                .flatMap(original -> stepsByOriginal.getOrDefault(original.getUuid(), List.of()).stream())
                .toList();
        String prefix = inferred ? "来源关系未完整建立，按同加工单母卷推定；" : "";
        return new SourceTrace(
                (inferred ? "推定来源：" : "") + originalSummary(originals),
                processModeText(originals),
                prefix + processSummary(steps, originals),
                originalSourceItems(originals, machineNameByUuid),
                processStepItems(steps));
    }

    private List<OriginalRoll> inferOriginals(FinishRoll finish, List<OriginalRoll> originals) {
        if (originals.isEmpty()) {
            return List.of();
        }
        if (finish == null) {
            return originals.size() == 1 ? List.of(originals.get(0)) : List.of();
        }
        List<OriginalRoll> byLabel = originals.stream()
                .filter(original -> matchOriginalLabel(finish, original))
                .toList();
        if (!byLabel.isEmpty()) {
            return byLabel;
        }
        if (originals.size() == 1) {
            return List.of(originals.get(0));
        }
        return inferBySortOrSpec(finish, originals);
    }

    private List<OriginalRoll> inferBySortOrSpec(FinishRoll finish, List<OriginalRoll> originals) {
        if (finish.getRowSort() != null) {
            List<OriginalRoll> bySort = originals.stream()
                    .filter(original -> finish.getRowSort().equals(original.getRowSort()))
                    .toList();
            if (!bySort.isEmpty()) {
                return bySort;
            }
        }
        List<OriginalRoll> byPaper = originals.stream()
                .filter(original -> samePaper(finish, original))
                .toList();
        return byPaper.size() == 1 ? byPaper : List.of();
    }

    private boolean matchOriginalLabel(FinishRoll finish, OriginalRoll original) {
        String labels = String.join(" ",
                text(finish.getOriginalRollNos()), text(finish.getFinishRollNo()));
        return containsLabel(labels, original.getRollNo())
                || containsLabel(labels, original.getExtraNo())
                || containsLabel(labels, original.getUuid());
    }

    private boolean containsLabel(String labels, String value) {
        return StringUtils.hasText(value) && labels.contains(value);
    }

    private boolean samePaper(FinishRoll finish, OriginalRoll original) {
        return text(finish.getPaperName()).equals(text(original.getPaperName()))
                && equalsValue(finish.getGramWeight(), original.getGramWeight());
    }

    private boolean equalsValue(Integer left, Integer right) {
        return left != null && left.equals(right);
    }

    private String resolveOrderUuid(DeliveryDetail detail, FinishRoll finish) {
        if (StringUtils.hasText(detail.getOrderUuid())) {
            return detail.getOrderUuid();
        }
        return finish == null ? null : finish.getOrderUuid();
    }

    private DeliveryDetailItemVO toDetailItem(DeliveryDetail detail, FinishRoll finish, ProcessOrder order,
                                             SourceTrace trace) {
        DeliveryDetailItemVO vo = new DeliveryDetailItemVO();
        vo.setUuid(detail.getUuid());
        vo.setDeliveryUuid(detail.getDeliveryUuid());
        vo.setFinishUuid(detail.getFinishUuid());
        vo.setOrderUuid(detail.getOrderUuid());
        vo.setOrderNo(order == null ? null : order.getOrderNo());
        vo.setFinishRollNo(finish == null ? detail.getFinishRollNo() : finish.getFinishRollNo());
        vo.setPaperName(finish == null ? detail.getPaperName() : finish.getPaperName());
        vo.setGramWeight(finish == null ? null : finish.getGramWeight());
        vo.setFinishWidth(finish == null ? null : finish.getFinishWidth());
        vo.setFinishDiameter(finish == null ? null : finish.getFinishDiameter());
        vo.setFinishCoreDiameter(finish == null ? null : finish.getFinishCoreDiameter());
        vo.setActualWeight(finish == null ? null : finish.getActualWeight());
        vo.setOutWeight(detail.getOutWeight());
        vo.setSourceType(finish == null ? null : finish.getSourceType());
        vo.setFinishStatus(finish == null ? null : finish.getFinishStatus());
        vo.setOriginalRollNos(finish == null ? null : finish.getOriginalRollNos());
        vo.setOriginalSummary(trace == null ? null : trace.originalSummary());
        vo.setProcessModeText(trace == null ? null : trace.processModeText());
        vo.setProcessSummary(trace == null ? null : trace.processSummary());
        vo.setOriginalItems(trace == null ? List.of() : trace.originalItems());
        vo.setProcessStepItems(trace == null ? List.of() : trace.processStepItems());
        vo.setRemark(detail.getRemark());
        vo.setFinishRemark(finish == null ? null : finish.getRemark());
        vo.setActualRemark(finish == null ? null : finish.getActualRemark());
        return vo;
    }

    private void ensureOrdersNotSettled(List<DeliveryDetail> details) {
        List<String> orderUuids = details.stream()
                .map(DeliveryDetail::getOrderUuid)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (orderUuids.isEmpty()) {
            return;
        }
        long settled = settleDetailMapper.selectCount(new LambdaQueryWrapper<SettleDetail>()
                .eq(SettleDetail::getIsDeleted, 0)
                .in(SettleDetail::getOrderUuid, orderUuids));
        if (settled > 0) {
            throw new BusinessException(ErrorCode.E004, "关联加工单已生成结算单，不可回退出库");
        }
    }

    private String originalSummary(List<OriginalRoll> originals) {
        if (originals.isEmpty()) {
            return "-";
        }
        return originals.stream()
                .map(this::originalText)
                .distinct()
                .reduce((left, right) -> left + "；" + right)
                .orElse("-");
    }

    private String originalText(OriginalRoll roll) {
        String label = StringUtils.hasText(roll.getRollNo()) ? roll.getRollNo() : "母卷" + roll.getRowSort();
        BigDecimal weight = roll.getActualWeight() != null ? roll.getActualWeight() : roll.getTotalWeight();
        return label + "｜" + roll.getPaperName() + " / " + roll.getGramWeight() + "g / "
                + roll.getOriginalWidth() + "mm｜" + nz(weight) + "kg";
    }

    private List<DeliveryDetailItemVO.OriginalSourceItem> originalSourceItems(
            List<OriginalRoll> originals, Map<String, String> machineNameByUuid) {
        return originals.stream().map(roll -> originalSourceItem(roll, machineNameByUuid)).toList();
    }

    private DeliveryDetailItemVO.OriginalSourceItem originalSourceItem(
            OriginalRoll roll, Map<String, String> machineNameByUuid) {
        DeliveryDetailItemVO.OriginalSourceItem item = new DeliveryDetailItemVO.OriginalSourceItem();
        item.setUuid(roll.getUuid());
        item.setRowSort(roll.getRowSort());
        item.setExtraNo(roll.getExtraNo());
        item.setRollNo(roll.getRollNo());
        item.setPaperName(roll.getPaperName());
        item.setGramWeight(roll.getGramWeight());
        item.setActualGramWeight(roll.getActualGramWeight());
        item.setOriginalWidth(roll.getOriginalWidth());
        item.setActualWidth(roll.getActualWidth());
        item.setActualWeight(roll.getActualWeight());
        item.setTotalWeight(roll.getTotalWeight());
        item.setProcessMode(roll.getProcessMode());
        item.setMainStepType(roll.getMainStepType());
        item.setMachineUuid(roll.getMachineUuid());
        item.setMachineName(machineNameByUuid.get(roll.getMachineUuid()));
        item.setOperator(roll.getOperator());
        item.setRemark(roll.getRemark());
        return item;
    }

    private String processModeText(List<OriginalRoll> originals) {
        return originals.stream()
                .map(this::processModeText)
                .distinct()
                .reduce((left, right) -> left + "；" + right)
                .orElse("-");
    }

    private String processModeText(OriginalRoll roll) {
        String mode = processModeText(roll.getProcessMode());
        String step = stepTypeText(roll.getMainStepType());
        return StringUtils.hasText(step) ? mode + " / " + step : mode;
    }

    private String processSummary(List<ProcessStep> steps, List<OriginalRoll> originals) {
        if (!steps.isEmpty()) {
            return steps.stream()
                    .map(this::stepText)
                    .distinct()
                    .reduce((left, right) -> left + "；" + right)
                    .orElse("-");
        }
        return originals.stream()
                .map(this::rollProcessSummary)
                .distinct()
                .reduce((left, right) -> left + "；" + right)
                .orElse("-");
    }

    private String rollProcessSummary(OriginalRoll roll) {
        if (roll.getProcessMode() != null && roll.getProcessMode() == 3) {
            return "直发出库";
        }
        String step = stepTypeText(roll.getMainStepType());
        if (StringUtils.hasText(step)) {
            return step + "（未记录工序明细）";
        }
        return processModeText(roll.getProcessMode()) + "（未记录工序明细）";
    }

    private String stepText(ProcessStep step) {
        if (step.getStepType() != null && step.getStepType() == 1) {
            return "锯纸 " + (step.getKnifeCount() == null ? 0 : step.getKnifeCount()) + "刀";
        }
        if (step.getStepType() != null && step.getStepType() == 2) {
            return "复卷 " + nz(step.getProcessWeight()) + "kg";
        }
        return StringUtils.hasText(step.getStepName()) ? step.getStepName() : "工序";
    }

    private List<DeliveryDetailItemVO.ProcessStepItem> processStepItems(List<ProcessStep> steps) {
        return steps.stream().map(this::processStepItem).toList();
    }

    private DeliveryDetailItemVO.ProcessStepItem processStepItem(ProcessStep step) {
        DeliveryDetailItemVO.ProcessStepItem item = new DeliveryDetailItemVO.ProcessStepItem();
        item.setUuid(step.getUuid());
        item.setOriginalUuid(step.getOriginalUuid());
        item.setStepSort(step.getStepSort());
        item.setStepType(step.getStepType());
        item.setStepName(step.getStepName());
        item.setIsMain(step.getIsMain());
        item.setKnifeCount(step.getKnifeCount());
        item.setProcessWeight(step.getProcessWeight());
        item.setUnitPrice(step.getUnitPrice());
        item.setStepAmount(step.getStepAmount());
        item.setLossWeight(step.getLossWeight());
        item.setOperator(step.getOperator());
        item.setRemark(step.getRemark());
        return item;
    }

    private String processModeText(Integer mode) {
        if (mode != null && mode == 2) return "现场定尺";
        if (mode != null && mode == 3) return "直发";
        return "标准加工";
    }

    private String stepTypeText(Integer type) {
        if (type != null && type == 1) return "锯纸";
        if (type != null && type == 2) return "复卷";
        return "";
    }

    private String sourceTypeText(FinishRoll finish) {
        if (finish == null) {
            return "-";
        }
        return finish.getSourceType() != null && finish.getSourceType() == 2 ? "直发原纸" : "加工产出";
    }

    private String unlinkedSourceText(DeliveryDetail detail, FinishRoll finish) {
        String orderUuid = resolveOrderUuid(detail, finish);
        return StringUtils.hasText(orderUuid) ? "来源未建立关联（加工单 " + orderUuid + "）" : "来源未建立关联";
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private void refreshTotals(DeliveryOrder order) {
        List<DeliveryDetail> details = deliveryDetails(order.getUuid());
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (DeliveryDetail detail : details) {
            totalWeight = totalWeight.add(nz(detail.getOutWeight()));
        }
        order.setTotalCount(details.size());
        order.setTotalWeight(totalWeight);
        ConcurrencyGuard.requireRowUpdated(getBaseMapper().update(null,
                new LambdaUpdateWrapper<DeliveryOrder>()
                        .eq(DeliveryOrder::getUuid, order.getUuid())
                        .eq(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_PENDING)
                        .set(DeliveryOrder::getTotalCount, order.getTotalCount())
                        .set(DeliveryOrder::getTotalWeight, order.getTotalWeight())
                        .set(DeliveryOrder::getSettleBlockAction, order.getSettleBlockAction())
                        .set(DeliveryOrder::getUpdateTime, LocalDateTime.now())
                        .set(DeliveryOrder::getUpdateBy, currentOperator())
                        .setSql("version = version + 1")));
    }

    private void updateFinishStatus(String finishUuid, int fromStatus, int toStatus) {
        ConcurrencyGuard.requireRowUpdated(finishRollMapper.update(null,
                new LambdaUpdateWrapper<FinishRoll>()
                        .eq(FinishRoll::getUuid, finishUuid)
                        .eq(FinishRoll::getFinishStatus, fromStatus)
                        .set(FinishRoll::getFinishStatus, toStatus)
                        .set(FinishRoll::getUpdateTime, LocalDateTime.now())
                        .set(FinishRoll::getUpdateBy, currentOperator())
                        .setSql("version = version + 1")));
    }

    private void updateDeliveryForConfirm(DeliveryOrder order) {
        ConcurrencyGuard.requireRowUpdated(getBaseMapper().update(null,
                new LambdaUpdateWrapper<DeliveryOrder>()
                        .eq(DeliveryOrder::getUuid, order.getUuid())
                        .eq(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_PENDING)
                        .set(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_OUT)
                        .set(DeliveryOrder::getSignUser, order.getSignUser())
                        .set(DeliveryOrder::getSignTime, order.getSignTime())
                        .set(DeliveryOrder::getRemark, order.getRemark())
                        .set(DeliveryOrder::getSnapDelivery, order.getSnapDelivery())
                        .set(DeliveryOrder::getSnapDeliveryTime, order.getSnapDeliveryTime())
                        .set(DeliveryOrder::getUpdateTime, LocalDateTime.now())
                        .set(DeliveryOrder::getUpdateBy, currentOperator())
                        .setSql("version = version + 1")));
    }

    private void updateDeliveryForRollback(DeliveryOrder order) {
        ConcurrencyGuard.requireRowUpdated(getBaseMapper().update(null,
                new LambdaUpdateWrapper<DeliveryOrder>()
                        .eq(DeliveryOrder::getUuid, order.getUuid())
                        .eq(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_OUT)
                        .set(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_PENDING)
                        .set(DeliveryOrder::getSignUser, null)
                        .set(DeliveryOrder::getSignTime, null)
                        .set(DeliveryOrder::getSnapDelivery, order.getSnapDelivery())
                        .set(DeliveryOrder::getSnapDeliveryTime, order.getSnapDeliveryTime())
                        .set(DeliveryOrder::getRemark, order.getRemark())
                        .set(DeliveryOrder::getUpdateTime, LocalDateTime.now())
                        .set(DeliveryOrder::getUpdateBy, currentOperator())
                        .setSql("version = version + 1")));
    }

    private String currentOperator() {
        return AuthContextHolder.currentDisplayName();
    }

    private String contentDisposition(String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename*=UTF-8''" + encoded;
    }

    private String appendRemark(String current, String extra) {
        if (!StringUtils.hasText(current)) {
            return extra;
        }
        String next = current + "；" + extra;
        return next.length() > 255 ? next.substring(next.length() - 255) : next;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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

    private List<OperationLog> loadOperationLogs(DeliveryOrder order) {
        List<OperationLog> logs = operationLogMapper.selectList(new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getBizType, OperationLogService.BIZ_TYPE_DELIVERY)
                .eq(OperationLog::getBizUuid, order.getUuid())
                .orderByDesc(OperationLog::getOperateTime));
        if (!logs.isEmpty()) {
            return logs;
        }
        return buildStatusLog(order);
    }

    private List<OperationLog> buildStatusLog(DeliveryOrder order) {
        OperationLog log = new OperationLog();
        log.setUuid("delivery-status-" + order.getUuid());
        log.setBizType(OperationLogService.BIZ_TYPE_DELIVERY);
        log.setBizUuid(order.getUuid());
        log.setBizNo(order.getDeliveryNo());
        log.setActionType(order.getDeliveryStatus() != null && order.getDeliveryStatus() == DELIVERY_STATUS_OUT
                ? OperationLogService.ACTION_DELIVERY_CONFIRM : "创建出库单");
        log.setOperator(StringUtils.hasText(order.getSignUser()) ? order.getSignUser() : "system");
        log.setOperateTime(order.getSignTime() != null ? order.getSignTime() : order.getCreateTime());
        log.setRemark(order.getDeliveryStatus() != null && order.getDeliveryStatus() == DELIVERY_STATUS_OUT
                ? "系统根据出库单签收状态生成追踪记录" : "系统根据出库单创建状态生成追踪记录");
        return List.of(log);
    }

    private record SourceTrace(
            String originalSummary,
            String processModeText,
            String processSummary,
            List<DeliveryDetailItemVO.OriginalSourceItem> originalItems,
            List<DeliveryDetailItemVO.ProcessStepItem> processStepItems
    ) {
    }
}
