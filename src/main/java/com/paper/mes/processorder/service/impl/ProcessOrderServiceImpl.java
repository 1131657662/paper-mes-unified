package com.paper.mes.processorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.service.AdminCredentialVerifier;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.ResultCode;
import com.paper.mes.common.PageResult;
import com.paper.mes.common.PageRequestBounds;
import com.paper.mes.common.audit.FieldAudit;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.machine.entity.Machine;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.calc.RewindWeightCalculator;
import com.paper.mes.processorder.calc.WeightCheckCalculator;
import com.paper.mes.processorder.dto.BackRecordDTO;
import com.paper.mes.processorder.dto.BackRecordFinishDTO;
import com.paper.mes.processorder.dto.BackRecordResultVO;
import com.paper.mes.processorder.dto.BackRecordRollDTO;
import com.paper.mes.processorder.dto.BackRecordStepDTO;
import com.paper.mes.processorder.dto.FeeResultVO;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSaveVO;
import com.paper.mes.processorder.dto.FinishConfigBatchSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigBatchSaveVO;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.FinishPreviewVO;
import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.dto.OriginalRollRemarkDTO;
import com.paper.mes.processorder.dto.PrintDTO;
import com.paper.mes.processorder.dto.PhysicalReprintDTO;
import com.paper.mes.processorder.dto.PrintResultVO;
import com.paper.mes.processorder.dto.PrintViewVersion;
import com.paper.mes.processorder.dto.ProcessOrderCreateDTO;
import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.dto.ProcessOrderPrintViewVO;
import com.paper.mes.processorder.dto.ProcessOrderQuery;
import com.paper.mes.processorder.dto.ProcessOrderRemarkDTO;
import com.paper.mes.processorder.dto.ProcessOrderVoidDTO;
import com.paper.mes.processorder.dto.ProcessStepDTO;
import com.paper.mes.processorder.dto.ProcessStepBatchDTO;
import com.paper.mes.processorder.dto.ProcessStepBatchResultVO;
import com.paper.mes.processorder.dto.ProcessStepPricingAdjustmentDTO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import com.paper.mes.processorder.dto.SnapshotDiffVO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessParam;
import com.paper.mes.processorder.entity.ProcessStageInputRel;
import com.paper.mes.processorder.entity.ProcessStageOutput;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.mapper.ProcessParamMapper;
import com.paper.mes.processorder.mapper.ProcessStageInputRelMapper;
import com.paper.mes.processorder.mapper.ProcessStageOutputMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import com.paper.mes.processorder.service.DamageImageService;
import com.paper.mes.processorder.service.BackRecordFinishRecorder;
import com.paper.mes.processorder.service.BackRecordOnSiteFinishRecorder;
import com.paper.mes.processorder.service.BackRecordOnSiteTrimRecorder;
import com.paper.mes.processorder.service.BackRecordScope;
import com.paper.mes.processorder.service.BackRecordScopeResolver;
import com.paper.mes.processorder.service.BackRecordReopenService;
import com.paper.mes.processorder.service.BackRecordWarehousePolicy;
import com.paper.mes.processorder.service.FinishRollSourceBinder;
import com.paper.mes.processorder.service.FinishCustomerSpecificationPolicy;
import com.paper.mes.processorder.service.FinishConfigQuantityValidator;
import com.paper.mes.processorder.service.MultiSourceConsumptionNormalizer;
import com.paper.mes.processorder.service.ProcessMixProcessResolver;
import com.paper.mes.processorder.service.ProcessModePolicy;
import com.paper.mes.processorder.service.ProcessCatalogStepValidator;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.processorder.service.ProcessRouteCleanupService;
import com.paper.mes.processorder.service.ProcessRouteContext;
import com.paper.mes.processorder.service.ProcessStepPricingSettings;
import com.paper.mes.processorder.service.ProcessStepPricingPolicy;
import com.paper.mes.processorder.service.ProcessStepEditStatusPolicy;
import com.paper.mes.processorder.service.ProcessStepPricingBatchCalculator;
import com.paper.mes.processorder.service.RollLossSummaryCalculator;
import com.paper.mes.processorder.service.RollNoSequenceService;
import com.paper.mes.processorder.service.SawPlanPreviewer;
import com.paper.mes.processorder.service.SawPlanCalculation;
import com.paper.mes.processorder.service.ServiceOnlyFinishFactory;
import com.paper.mes.processorder.service.ServiceStepBatchUpsertWriter;
import com.paper.mes.processorder.service.ServiceOnlyProcessPolicy;
import com.paper.mes.processorder.service.ServiceStepQuantityResolver;
import com.paper.mes.processorder.service.WeightCheckThresholdService;
import com.paper.mes.processorder.statemachine.OrderStatus;
import com.paper.mes.processorder.statemachine.RollStatus;
import com.paper.mes.processorder.statemachine.StateMachine;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import com.paper.mes.system.config.constant.NoRuleBizType;
import com.paper.mes.system.config.service.DocumentNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProcessOrderServiceImpl extends ServiceImpl<ProcessOrderMapper, ProcessOrder>
        implements ProcessOrderService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_PROCESSING = 2;
    private static final int STATUS_TO_RECORD = 3;
    private static final int STATUS_FINISHED = 4;
    private static final int STATUS_SETTLED = 5;
    private static final int STATUS_VOIDED = 6;
    private static final int STEP_MAIN = 1;
    private static final int ROLL_STATUS_PENDING = 1;
    private static final int DEFAULT_PIECE_NUM = 1;
    private static final int DEFAULT_PROCESS_MODE = 1;
    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final int PROCESS_MODE_ON_SITE = 2;
    private static final int PRINT_STATUS_UNPRINTED = 0;
    private static final int PRINT_STATUS_PRINTED = 1;
    private static final int IS_SPARE_NO = 0;
    private static final int IS_SPARE_YES = 1;
    private static final int IS_REMAIN_NO = 0;
    private static final int IS_REMAIN_YES = 1;
    private static final int ROLL_NO_VOID = 3;
    private static final int ROLL_NO_PRE = 1;
    private static final int PROCESS_MODE_DIRECT_SHIP = 3;
    private static final int SOURCE_DIRECT_SHIP = 2;
    private static final int FINISH_STATUS_PENDING = 1;
    private static final int FINISH_STATUS_IN_STOCK = 2;
    private static final int FINISH_STATUS_OUT = 3;
    private static final int DEFAULT_SETTLE_TYPE = 2;
    private static final int DEFAULT_IS_INVOICE = 2;
    private static final String BIZ_TYPE_ORDER = "加工单";
    /** 快照结构版本，写入 snap_print 根节点；缺失则拒绝写入（V4.1 §6.1）。 */
    private static final String SNAP_SCHEMA_VERSION = "2.0";
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal LEGACY_DEFAULT_SAW_PRICE = new BigDecimal("1.50");
    private static final BigDecimal LEGACY_DEFAULT_REWIND_PRICE = new BigDecimal("200.00");
    private static final String LAYOUT_ITEM_FINISH = "FINISH";
    private static final String LAYOUT_ITEM_TRIM = "TRIM";

    private final OriginalRollMapper originalRollMapper;
    private final FinishRollMapper finishRollMapper;
    private final ProcessStepMapper processStepMapper;
    private final ProcessParamMapper processParamMapper;
    private final ProcessStageInputRelMapper processStageInputRelMapper;
    private final ProcessStageOutputMapper processStageOutputMapper;
    private final FinishOriginalRelMapper finishOriginalRelMapper;
    private final DeliveryDetailMapper deliveryDetailMapper;
    private final SettleDetailMapper settleDetailMapper;
    private final CustomerService customerService;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;
    private final DamageImageService damageImageService;
    private final RollNoSequenceService rollNoSequenceService;
    private final SawPlanPreviewer sawPlanPreviewer;
    private final DocumentNoService documentNoService;
    private final BusinessLockService businessLockService;
    private final MachineMapper machineMapper;
    private final WeightCheckThresholdService weightCheckThresholdService;
    private final BackRecordFinishRecorder backRecordFinishRecorder;
    private final BackRecordOnSiteFinishRecorder backRecordOnSiteFinishRecorder;
    private final BackRecordOnSiteTrimRecorder backRecordOnSiteTrimRecorder;
    private final BackRecordScopeResolver backRecordScopeResolver;
    private final BackRecordReopenService backRecordReopenService;
    private final BackRecordWarehousePolicy backRecordWarehousePolicy;
    private final FinishRollSourceBinder finishRollSourceBinder;
    private final AdminCredentialVerifier adminCredentialVerifier;
    private final PermissionChecker permissionChecker;
    private final ProcessStepPricingSettings processStepPricingSettings;
    private final ProcessCatalogStepValidator processCatalogStepValidator;
    private final ServiceStepBatchUpsertWriter serviceStepBatchUpsertWriter;
    private final ServiceOnlyProcessPolicy serviceOnlyProcessPolicy;
    private final ProcessRouteCleanupService processRouteCleanupService;

    @Override
    public PageResult<ProcessOrder> pageOrders(ProcessOrderQuery query) {
        LambdaQueryWrapper<ProcessOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(ProcessOrder::getOrderNo, kw)
                    .or().like(ProcessOrder::getCustomerName, kw));
        }
        if (query.getOrderStatus() != null) {
            wrapper.eq(ProcessOrder::getOrderStatus, query.getOrderStatus());
        } else {
            wrapper.ne(ProcessOrder::getOrderStatus, STATUS_VOIDED);
        }
        if (StringUtils.hasText(query.getCustomerUuid())) {
            wrapper.eq(ProcessOrder::getCustomerUuid, query.getCustomerUuid());
        }
        if (query.getDateFrom() != null) {
            wrapper.ge(ProcessOrder::getOrderDate, query.getDateFrom());
        }
        if (query.getDateTo() != null) {
            wrapper.le(ProcessOrder::getOrderDate, query.getDateTo());
        }
        // 列表分页不查大字段：snap_print/snap_finish（完整快照 JSON）与 remark_long（TEXT）
        // 仅详情/快照对比/打印需要，故只在此投影排除，不用全局 @TableField(select=false)。
        wrapper.select(ProcessOrder.class, info -> {
            String c = info.getColumn();
            return !"snap_print".equals(c) && !"snap_finish".equals(c) && !"remark_long".equals(c);
        });
        wrapper.orderByDesc(ProcessOrder::getCreateTime);
        Page<ProcessOrder> page = page(PageRequestBounds.of(query.getCurrent(), query.getSize()), wrapper);
        fillListStats(page.getRecords());
        return PageResult.of(page);
    }

    private void fillListStats(List<ProcessOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        List<String> orderUuids = orders.stream().map(ProcessOrder::getUuid).toList();
        Map<String, List<OriginalRoll>> originals = loadOriginalsByOrder(orderUuids);
        Map<String, List<FinishRoll>> finishes = loadFinishesByOrder(orderUuids);
        Map<String, List<ProcessStep>> steps = loadStepsByOrder(orderUuids);
        for (ProcessOrder order : orders) {
            List<OriginalRoll> orderRolls = originals.get(order.getUuid());
            ProcessOrderListStats.apply(order, orderRolls, finishes.get(order.getUuid()));
            order.setProcessNames(processNames(steps.get(order.getUuid()), orderRolls));
            if (order.getSettleType() == null) order.setSettleType(2);
            if (order.getIsInvoice() == null) order.setIsInvoice(2);
        }
    }

    private Map<String, List<OriginalRoll>> loadOriginalsByOrder(List<String> orderUuids) {
        List<OriginalRoll> rolls = originalRollMapper.selectList(new LambdaQueryWrapper<OriginalRoll>()
                .in(OriginalRoll::getOrderUuid, orderUuids));
        Map<String, List<OriginalRoll>> grouped = new LinkedHashMap<>();
        for (OriginalRoll roll : rolls) {
            grouped.computeIfAbsent(roll.getOrderUuid(), key -> new ArrayList<>()).add(roll);
        }
        return grouped;
    }

    private Map<String, List<FinishRoll>> loadFinishesByOrder(List<String> orderUuids) {
        List<FinishRoll> rolls = finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .in(FinishRoll::getOrderUuid, orderUuids));
        Map<String, List<FinishRoll>> grouped = new LinkedHashMap<>();
        for (FinishRoll roll : rolls) {
            grouped.computeIfAbsent(roll.getOrderUuid(), key -> new ArrayList<>()).add(roll);
        }
        return grouped;
    }

    private Map<String, List<ProcessStep>> loadStepsByOrder(List<String> orderUuids) {
        List<ProcessStep> steps = processStepMapper.selectList(new LambdaQueryWrapper<ProcessStep>()
                .in(ProcessStep::getOrderUuid, orderUuids)
                .select(ProcessStep::getOrderUuid, ProcessStep::getOriginalUuid,
                        ProcessStep::getStepType, ProcessStep::getStepName,
                        ProcessStep::getStepSort, ProcessStep::getIsMain)
                .orderByAsc(ProcessStep::getOrderUuid)
                .orderByDesc(ProcessStep::getIsMain)
                .orderByAsc(ProcessStep::getStepSort, ProcessStep::getOriginalUuid));
        Map<String, List<ProcessStep>> grouped = new LinkedHashMap<>();
        for (ProcessStep step : steps) {
            grouped.computeIfAbsent(step.getOrderUuid(), key -> new ArrayList<>()).add(step);
        }
        return grouped;
    }

    static List<String> processNames(List<ProcessStep> steps, List<OriginalRoll> rolls) {
        return ProcessOrderListStats.processNames(steps, rolls);
    }

    private boolean isFormalFinishRoll(FinishRoll roll) {
        return ProcessOrderListStats.isFormalFinishRoll(roll);
    }

    @Override
    public ProcessOrderDetailVO getDetail(String uuid) {
        ProcessOrder order = getById(uuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        List<OriginalRoll> rolls = originalRollMapper.selectList(
                new LambdaQueryWrapper<OriginalRoll>()
                        .eq(OriginalRoll::getOrderUuid, uuid)
                        .orderByAsc(OriginalRoll::getRowSort));
        List<FinishRoll> finishRolls = finishRollMapper.selectList(
                new LambdaQueryWrapper<FinishRoll>()
                        .eq(FinishRoll::getOrderUuid, uuid)
                        .orderByAsc(FinishRoll::getRowSort));
        List<ProcessStep> steps = processStepMapper.selectList(
                new LambdaQueryWrapper<ProcessStep>()
                        .eq(ProcessStep::getOrderUuid, uuid)
                        .orderByAsc(ProcessStep::getStepSort));
        sortStepsForDetail(steps, rolls);
        List<ProcessParam> processParams = processParamMapper.selectList(
                new LambdaQueryWrapper<ProcessParam>()
                        .eq(ProcessParam::getOrderUuid, uuid)
                        .orderByAsc(ProcessParam::getOriginalUuid)
                        .orderByAsc(ProcessParam::getLayerSort));
        List<ProcessStageOutput> stageOutputs = processStageOutputMapper.selectList(
                new LambdaQueryWrapper<ProcessStageOutput>()
                        .eq(ProcessStageOutput::getOrderUuid, uuid)
                        .orderByAsc(ProcessStageOutput::getOriginalUuid)
                        .orderByAsc(ProcessStageOutput::getStageLevel)
                        .orderByAsc(ProcessStageOutput::getOutputSort));
        List<FinishOriginalRel> finishOriginalRels = finishOriginalRelMapper.selectList(
                new LambdaQueryWrapper<FinishOriginalRel>()
                        .eq(FinishOriginalRel::getOrderUuid, uuid));
        ProcessOrderDetailVO vo = new ProcessOrderDetailVO();
        vo.setOrder(order);
        vo.setOriginalRolls(rolls);
        vo.setFinishRolls(finishRolls);
        vo.setSteps(steps);
        vo.setRollProductions(buildRollProductions(rolls, finishRolls, steps, processParams, stageOutputs, finishOriginalRels));
        return vo;
    }

    static void sortStepsForDetail(List<ProcessStep> steps, List<OriginalRoll> rolls) {
        Map<String, Integer> rollSortByUuid = new LinkedHashMap<>();
        for (OriginalRoll roll : rolls) {
            rollSortByUuid.put(roll.getUuid(), roll.getRowSort() == null ? Integer.MAX_VALUE : roll.getRowSort());
        }
        Comparator<Integer> nullableInteger = Comparator.nullsLast(Integer::compareTo);
        steps.sort(Comparator
                .comparingInt((ProcessStep step) -> rollSortByUuid.getOrDefault(step.getOriginalUuid(), Integer.MAX_VALUE))
                .thenComparing(ProcessStep::getStageLevel, nullableInteger)
                .thenComparing(ProcessStep::getStepSort, nullableInteger)
                .thenComparing(ProcessStep::getUuid, Comparator.nullsLast(String::compareTo)));
    }

    @Override
    public ProcessOrderPrintViewVO getPrintView(String uuid, PrintViewVersion version) {
        if (version == null) {
            throw new BusinessException("打印版本不能为空");
        }
        return ProcessOrderPrintViewReader.read(getDetail(uuid), version, objectMapper);
    }

    private List<ProcessOrderDetailVO.RollProductionVO> buildRollProductions(List<OriginalRoll> rolls,
                                                                             List<FinishRoll> finishRolls,
                                                                             List<ProcessStep> steps,
                                                                             List<ProcessParam> processParams,
                                                                             List<ProcessStageOutput> stageOutputs,
                                                                             List<FinishOriginalRel> finishOriginalRels) {
        Map<String, OriginalRoll> rollByUuid = new LinkedHashMap<>();
        for (OriginalRoll roll : rolls) {
            rollByUuid.put(roll.getUuid(), roll);
        }
        Map<String, List<ProcessStep>> stepsByRoll = groupStepsByRoll(steps);
        Map<String, List<ProcessParam>> paramsByRoll = groupParamsByRoll(processParams);
        Map<String, List<ProcessStageOutput>> outputsByRoll = groupStageOutputsByRoll(stageOutputs);
        Map<String, List<FinishOriginalRel>> relsByFinish = groupRelsByFinish(finishOriginalRels);
        Map<String, List<FinishRoll>> finishesByRoll = groupFinishesByRoll(finishRolls, finishOriginalRels, rollByUuid);
        Map<String, FinishRoll> finishByUuid = indexFinishesByUuid(finishRolls);

        List<ProcessOrderDetailVO.RollProductionVO> productions = new ArrayList<>(rolls.size());
        for (OriginalRoll roll : rolls) {
            ProcessOrderDetailVO.RollProductionVO item = new ProcessOrderDetailVO.RollProductionVO();
            item.setOriginalUuid(roll.getUuid());
            item.setExtraNo(roll.getExtraNo());
            item.setBatchNo(roll.getBatchNo());
            item.setRollNo(roll.getRollNo());
            item.setDamageDesc(roll.getDamageDesc());
            item.setDamageImages(parseDamageImages(roll.getDamageImages()));
            item.setPaperName(roll.getPaperName());
            item.setGramWeight(roll.getGramWeight());
            item.setActualGramWeight(roll.getActualGramWeight());
            item.setOriginalWidth(roll.getOriginalWidth());
            item.setActualWidth(roll.getActualWidth());
            item.setRollWeight(roll.getRollWeight());
            item.setActualWeight(roll.getActualWeight());
            item.setProcessAmount(roll.getProcessAmount());
            item.setPieceNum(roll.getPieceNum());
            item.setProcessMode(roll.getProcessMode());
            item.setMainStepType(roll.getMainStepType());
            item.setRollStatus(roll.getRollStatus());
            item.setIsChecked(roll.getIsChecked());
            item.setCheckUser(roll.getCheckUser());
            item.setCheckTime(roll.getCheckTime());
            item.setRemark(roll.getRemark());
            item.setSteps(stepsByRoll.getOrDefault(roll.getUuid(), List.of()));
            item.setStageOutputs(toDetailStageOutputs(outputsByRoll.get(roll.getUuid()), finishByUuid));
            item.setRewindParams(toDetailRewindParams(paramsByRoll.get(roll.getUuid())));
            item.setFinishes(toDetailFinishes(finishesByRoll.get(roll.getUuid()), relsByFinish, rollByUuid));
            productions.add(item);
        }
        return productions;
    }

    private Map<String, FinishRoll> indexFinishesByUuid(List<FinishRoll> finishes) {
        Map<String, FinishRoll> result = new LinkedHashMap<>();
        for (FinishRoll finish : finishes) {
            result.put(finish.getUuid(), finish);
        }
        return result;
    }

    private Map<String, List<FinishRoll>> groupFinishesByRoll(List<FinishRoll> finishes,
                                                              List<FinishOriginalRel> rels,
                                                              Map<String, OriginalRoll> rollByUuid) {
        Map<String, FinishRoll> finishByUuid = new LinkedHashMap<>();
        for (FinishRoll finish : finishes) {
            finishByUuid.put(finish.getUuid(), finish);
        }
        Map<String, List<FinishRoll>> grouped = new LinkedHashMap<>();
        for (FinishOriginalRel rel : rels) {
            FinishRoll finish = finishByUuid.get(rel.getFinishUuid());
            if (finish != null && rollByUuid.containsKey(rel.getOriginalUuid())) {
                grouped.computeIfAbsent(rel.getOriginalUuid(), k -> new ArrayList<>()).add(finish);
            }
        }
        for (FinishRoll finish : finishes) {
            if (finish.getSourceType() == SOURCE_DIRECT_SHIP) {
                for (OriginalRoll roll : rollByUuid.values()) {
                    if (finishOriginalKey(roll).equals(finish.getOriginalRollNos())) {
                        grouped.computeIfAbsent(roll.getUuid(), k -> new ArrayList<>()).add(finish);
                        break;
                    }
                }
            }
        }
        return grouped;
    }

    private Map<String, List<ProcessStageOutput>> groupStageOutputsByRoll(List<ProcessStageOutput> outputs) {
        Map<String, List<ProcessStageOutput>> grouped = new LinkedHashMap<>();
        for (ProcessStageOutput output : outputs) {
            grouped.computeIfAbsent(output.getOriginalUuid(), key -> new ArrayList<>()).add(output);
        }
        return grouped;
    }

    private List<ProcessOrderDetailVO.StageOutputVO> toDetailStageOutputs(List<ProcessStageOutput> outputs,
                                                                          Map<String, FinishRoll> finishByUuid) {
        if (outputs == null || outputs.isEmpty()) {
            return List.of();
        }
        List<ProcessOrderDetailVO.StageOutputVO> result = new ArrayList<>(outputs.size());
        for (ProcessStageOutput output : outputs) {
            result.add(toDetailStageOutput(output, finishByUuid));
        }
        return result;
    }

    private ProcessOrderDetailVO.StageOutputVO toDetailStageOutput(ProcessStageOutput output,
                                                                   Map<String, FinishRoll> finishByUuid) {
        ProcessOrderDetailVO.StageOutputVO item = new ProcessOrderDetailVO.StageOutputVO();
        item.setUuid(output.getUuid());
        item.setOutputNo(output.getOutputNo());
        item.setFinishRollUuid(output.getFinishRollUuid());
        item.setParentOutputUuid(output.getParentOutputUuid());
        item.setStageLevel(output.getStageLevel());
        item.setOutputSort(output.getOutputSort());
        item.setOutputType(output.getOutputType());
        item.setOutputStatus(output.getOutputStatus());
        item.setPaperName(output.getPaperName());
        item.setGramWeight(output.getGramWeight());
        item.setFinishWidth(output.getFinishWidth());
        item.setFinishDiameter(output.getFinishDiameter());
        item.setFinishCoreDiameter(output.getFinishCoreDiameter());
        item.setEstimateWeight(output.getEstimateWeight());
        item.setActualWeight(resolveStageActualWeight(output, finishByUuid));
        item.setIsRemain(isTrimStageOutput(output) ? IS_REMAIN_YES : IS_REMAIN_NO);
        item.setSourceStepType(output.getSourceStepType());
        item.setSourceSummary(output.getSourceSummary());
        item.setRemark(output.getRemark());
        return item;
    }

    private boolean isTrimStageOutput(ProcessStageOutput output) {
        return "修边/余料".equals(output.getRemark())
                || "修边/余料".equals(output.getPaperName())
                || "修边".equals(output.getPaperName())
                || "切边".equals(output.getPaperName())
                || "修边".equals(output.getOutputNo())
                || "切边".equals(output.getOutputNo());
    }

    private BigDecimal resolveStageActualWeight(ProcessStageOutput output, Map<String, FinishRoll> finishByUuid) {
        if (output.getActualWeight() != null) {
            return output.getActualWeight();
        }
        FinishRoll finish = finishByUuid.get(output.getFinishRollUuid());
        return finish == null ? null : finish.getActualWeight();
    }

    private List<ProcessOrderDetailVO.RewindParamVO> toDetailRewindParams(List<ProcessParam> params) {
        if (params == null || params.isEmpty()) {
            return List.of();
        }
        List<ProcessOrderDetailVO.RewindParamVO> result = new ArrayList<>(params.size());
        for (ProcessParam param : params) {
            ProcessOrderDetailVO.RewindParamVO item = new ProcessOrderDetailVO.RewindParamVO();
            item.setParamMode(param.getParamMode());
            item.setLayerSort(param.getLayerSort());
            item.setOutDiameter(param.getOutDiameter());
            item.setCoreDiameter(param.getCoreDiameter());
            item.setLayerWidth(param.getLayerWidth());
            item.setAreaRatio(param.getAreaRatio());
            item.setSplitRatio(param.getSplitRatio());
            item.setRemark(param.getRemark());
            result.add(item);
        }
        return result;
    }

    private List<ProcessOrderDetailVO.FinishProductionVO> toDetailFinishes(List<FinishRoll> finishes,
                                                                           Map<String, List<FinishOriginalRel>> relsByFinish,
                                                                           Map<String, OriginalRoll> rollByUuid) {
        if (finishes == null || finishes.isEmpty()) {
            return List.of();
        }
        List<ProcessOrderDetailVO.FinishProductionVO> result = new ArrayList<>(finishes.size());
        Set<String> added = new LinkedHashSet<>();
        for (FinishRoll finish : finishes) {
            if (!added.add(finish.getUuid())) {
                continue;
            }
            ProcessOrderDetailVO.FinishProductionVO item = new ProcessOrderDetailVO.FinishProductionVO();
            item.setUuid(finish.getUuid());
            item.setFinishRollNo(finish.getFinishRollNo());
            item.setRowSort(finish.getRowSort());
            item.setRollNoStatus(finish.getRollNoStatus());
            item.setIsSpare(finish.getIsSpare());
            item.setIsRemain(finish.getIsRemain());
            item.setSourceType(finish.getSourceType());
            item.setPaperName(finish.getPaperName());
            item.setGramWeight(finish.getGramWeight());
            item.setFinishWidth(finish.getFinishWidth());
            item.setFinishDiameter(finish.getFinishDiameter());
            item.setFinishCoreDiameter(finish.getFinishCoreDiameter());
            item.setEstimateWeight(finish.getEstimateWeight());
            item.setActualWeight(finish.getActualWeight());
            item.setTrimWidthShare(finish.getTrimWidthShare());
            item.setTrimWeightShare(finish.getTrimWeightShare());
            item.setActualRemark(finish.getActualRemark());
            item.setFinishStatus(finish.getFinishStatus());
            item.setSources(toDetailSources(relsByFinish.get(finish.getUuid()), rollByUuid));
            result.add(item);
        }
        return result;
    }

    private List<ProcessOrderDetailVO.FinishSourceVO> toDetailSources(List<FinishOriginalRel> rels,
                                                                      Map<String, OriginalRoll> rollByUuid) {
        if (rels == null || rels.isEmpty()) {
            return List.of();
        }
        List<ProcessOrderDetailVO.FinishSourceVO> result = new ArrayList<>(rels.size());
        for (FinishOriginalRel rel : rels) {
            OriginalRoll roll = rollByUuid.get(rel.getOriginalUuid());
            ProcessOrderDetailVO.FinishSourceVO item = new ProcessOrderDetailVO.FinishSourceVO();
            item.setOriginalUuid(rel.getOriginalUuid());
            item.setRowSort(roll == null ? null : roll.getRowSort());
            item.setExtraNo(roll == null ? null : roll.getExtraNo());
            item.setRollNo(roll == null ? null : roll.getRollNo());
            item.setPaperName(roll == null ? null : roll.getPaperName());
            item.setGramWeight(roll == null ? null : roll.getGramWeight());
            item.setActualGramWeight(roll == null ? null : roll.getActualGramWeight());
            item.setOriginalWidth(roll == null ? null : roll.getOriginalWidth());
            item.setActualWidth(roll == null ? null : roll.getActualWidth());
            item.setRollWeight(roll == null ? null : roll.getRollWeight());
            item.setPieceNum(roll == null ? null : roll.getPieceNum());
            item.setActualWeight(roll == null ? null : roll.getActualWeight());
            item.setTotalWeight(roll == null ? null : roll.getTotalWeight());
            item.setShareRatio(rel.getShareRatio());
            item.setShareWeight(rel.getShareWeight());
            item.setRemark(rel.getRemark());
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ProcessOrderCreateDTO dto) {
        Customer customer = customerService.getById(dto.getCustomerUuid());
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }
        validateCreateMainProcesses(dto.getOriginalRolls());

        ProcessOrder order = new ProcessOrder();
        BeanUtils.copyProperties(dto, order, "originalRolls");
        order.setCustomerName(customer.getCustomerName());
        applyCustomerDefaults(dto, order, customer);
        order.setOrderNo(nextOrderNo(dto.getOrderDate()));
        order.setOrderStatus(STATUS_PENDING);
        save(order);

        for (int i = 0; i < dto.getOriginalRolls().size(); i++) {
            OriginalRollDTO rollDto = dto.getOriginalRolls().get(i);
            OriginalRoll roll = buildRoll(rollDto, order, i + 1);
            originalRollMapper.insert(roll);
            createMainStepIfNeeded(order, roll);
        }

        updateMixProcessFlag(order.getUuid());
        return order.getUuid();
    }

    private void applyCustomerDefaults(ProcessOrderCreateDTO dto, ProcessOrder order, Customer customer) {
        Integer customerSettleType = customer.getSettleType() == null ? DEFAULT_SETTLE_TYPE : customer.getSettleType();
        Integer customerInvoice = customer.getDefaultInvoice() == null ? DEFAULT_IS_INVOICE : customer.getDefaultInvoice();
        Integer settleType = dto.getSettleType() == null ? customerSettleType : dto.getSettleType();
        order.setSettleType(settleType);
        order.setSettleDay(settleType == DEFAULT_SETTLE_TYPE
                ? (dto.getSettleDay() == null ? customer.getSettleDay() : dto.getSettleDay())
                : null);
        order.setIsInvoice(dto.getIsInvoice() == null ? customerInvoice : dto.getIsInvoice());
        order.setTaxRate(dto.getTaxRate() == null ? BigDecimal.ZERO : dto.getTaxRate());
        if (dto.getTaxRate() == null && customer.getTaxRate() != null) {
            order.setTaxRate(customer.getTaxRate());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String addRoll(String orderUuid, OriginalRollDTO dto) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = requireOrder(orderUuid);
        validateRollStructureEditable(order);
        OriginalRoll roll = buildRoll(dto, order, nextRowSort(orderUuid));
        validateMainProcessCatalog(roll);
        originalRollMapper.insert(roll);
        createMainStepIfNeeded(order, roll);
        updateMixProcessFlag(orderUuid);
        calcFee(orderUuid);
        return roll.getUuid();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderRemark(String uuid, ProcessOrderRemarkDTO dto) {
        businessLockService.lockProcessOrders(List.of(uuid));
        ProcessOrder order = requireOrder(uuid);
        validateRemarkEditable(order);
        String operator = currentOperator();
        LocalDateTime now = LocalDateTime.now();
        ConcurrencyGuard.requireRowUpdated(getBaseMapper().update(null,
                new LambdaUpdateWrapper<ProcessOrder>()
                        .eq(ProcessOrder::getUuid, uuid)
                        .set(ProcessOrder::getRemark, dto.getRemark())
                        .set(ProcessOrder::getRemarkLong, dto.getRemarkLong())
                        .set(ProcessOrder::getUpdateBy, operator)
                        .set(ProcessOrder::getUpdateTime, now)
                        .setSql("version = version + 1")));
        recordFieldIfChanged(order.getUuid(), order.getOrderNo(), "主单备注", order.getRemark(), dto.getRemark(), operator);
        recordFieldIfChanged(order.getUuid(), order.getOrderNo(), "主单详细备注", order.getRemarkLong(), dto.getRemarkLong(), operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @FieldAudit(bizType = "加工单", entity = OriginalRoll.class)
    public void updateRoll(String rollUuid, OriginalRollDTO dto) {
        LockedRoll locked = lockRollAndOrder(rollUuid);
        validateRollStructureEditable(locked.order());
        OriginalRoll existing = locked.roll();
        OriginalRoll updated = new OriginalRoll();
        BeanUtils.copyProperties(existing, updated);
        Integer savedVersion = existing.getVersion();
        Integer keepRowSort = existing.getRowSort();
        Integer keepStatus = existing.getRollStatus();
        BeanUtils.copyProperties(dto, updated);
        updated.setUuid(rollUuid);
        updated.setVersion(savedVersion);
        updated.setRowSort(keepRowSort);
        updated.setRollStatus(keepStatus);
        if (updated.getPieceNum() == null) {
            updated.setPieceNum(DEFAULT_PIECE_NUM);
        }
        if (updated.getProcessMode() == null) {
            updated.setProcessMode(DEFAULT_PROCESS_MODE);
        }
        updated.setTotalWeight(calcTotalWeight(updated.getRollWeight(), updated.getPieceNum()));
        validateMainStepType(updated);
        validateMainProcessCatalog(updated);
        if (hasStructuralRollChange(existing, updated)) {
            processRouteCleanupService.clearExistingRoute(new ProcessRouteContext(locked.order(), existing));
        }
        ConcurrencyGuard.requireRowUpdated(originalRollMapper.updateById(updated));
        if (hasStructuralRollChange(existing, updated)) {
            createMainStepIfNeeded(locked.order(), updated);
            calcFee(locked.order().getUuid());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRollRemark(String rollUuid, OriginalRollRemarkDTO dto) {
        LockedRoll locked = lockRollAndOrder(rollUuid);
        OriginalRoll roll = locked.roll();
        ProcessOrder order = locked.order();
        validateRemarkEditable(order);
        String operator = currentOperator();
        LocalDateTime now = LocalDateTime.now();
        ConcurrencyGuard.requireRowUpdated(originalRollMapper.update(null,
                new LambdaUpdateWrapper<OriginalRoll>()
                        .eq(OriginalRoll::getUuid, rollUuid)
                        .set(OriginalRoll::getBatchNo, dto.getBatchNo())
                        .set(OriginalRoll::getDamageDesc, dto.getDamageDesc())
                        .set(OriginalRoll::getRemark, dto.getRemark())
                        .set(OriginalRoll::getUpdateBy, operator)
                        .set(OriginalRoll::getUpdateTime, now)
                        .setSql("version = version + 1")));
        recordFieldIfChanged(order.getUuid(), order.getOrderNo(), rollLabel(roll) + " 批次", roll.getBatchNo(), dto.getBatchNo(), operator);
        recordFieldIfChanged(order.getUuid(), order.getOrderNo(), rollLabel(roll) + " 损伤说明", roll.getDamageDesc(), dto.getDamageDesc(), operator);
        recordFieldIfChanged(order.getUuid(), order.getOrderNo(), rollLabel(roll) + " 明细备注", roll.getRemark(), dto.getRemark(), operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoll(String rollUuid) {
        LockedRoll locked = lockRollAndOrder(rollUuid);
        validateRollStructureEditable(locked.order());
        processRouteCleanupService.clearExistingRoute(
                new ProcessRouteContext(locked.order(), locked.roll()));
        ConcurrencyGuard.requireRowUpdated(originalRollMapper.deleteById(rollUuid));
        calcFee(locked.order().getUuid());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinishConfigSaveVO saveFinishConfig(String orderUuid, String rollUuid, FinishConfigSaveDTO dto) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        FinishConfigSaveVO result = saveFinishConfigInternal(orderUuid, rollUuid, dto);
        calcFee(orderUuid);
        return result;
    }

    private FinishConfigSaveVO saveFinishConfigInternal(String orderUuid, String rollUuid, FinishConfigSaveDTO dto) {
        FinishConfigQuantityValidator.requireWithinLimit(dto);
        ProcessOrder order = getById(orderUuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        if (order.getOrderStatus() != STATUS_PENDING) {
            throw new BusinessException(ErrorCode.E001, "只能在待下发状态配置成品");
        }

        OriginalRoll roll = originalRollMapper.selectById(rollUuid);
        if (roll == null || !orderUuid.equals(roll.getOrderUuid())) {
            throw new BusinessException(ErrorCode.E002, "原纸明细不存在");
        }

        roll.setProcessMode(dto.getProcessMode());
        roll.setMainStepType(dto.getMainStepType());
        if (StringUtils.hasText(dto.getMachineUuid())) {
            roll.setMachineUuid(dto.getMachineUuid());
        }
        validateFinishConfig(orderUuid, roll, dto);
        validateMainStepType(roll);
        validateMainProcessCatalog(roll);
        if (isServiceOnly(roll)) serviceOnlyProcessPolicy.requireConfiguredStep(roll.getUuid());
        ConcurrencyGuard.requireRowUpdated(originalRollMapper.updateById(roll));

        voidExistingFinishConfig(orderUuid, roll);
        ProcessStep mainStep = syncMainStep(order, roll, dto);
        voidExistingRewindConfig(orderUuid, roll);

        List<String> finishRollNos = new ArrayList<>();
        List<String> spareRollNos = new ArrayList<>();
        if (isServiceOnly(roll)) {
            finishRollNos.addAll(createServiceOnlyFinishes(order, roll));
        } else if (!isDirectShip(roll)) {
            List<FinishConfigSpecDTO> specs = roll.getMainStepType() == FeeCalculator.STEP_TYPE_REWIND
                    ? buildRewindSaveSpecs(orderUuid, roll, dto)
                    : buildSawSaveSpecs(roll, dto);
            if (roll.getMainStepType() == FeeCalculator.STEP_TYPE_REWIND) {
                saveRewindParams(order, roll, mainStep, dto, specs);
            }
            int rowSort = nextFinishRowSort(orderUuid);
            for (FinishConfigSpecDTO spec : specs) {
                for (int i = 0; i < spec.getCount(); i++) {
                    FinishRoll finish = buildFinishRoll(order, roll, spec, rowSort++, IS_SPARE_NO);
                    String finishRollNo = allocAndInsertFinish(finish);
                    finishRollNos.add(finishRollNo);
                    saveFinishOriginalRelIfNeeded(order, roll, dto, spec, finish);
                }
            }
            int spareCount = isOnSite(roll) ? 0 : dto.getSpareCount() == null ? 0 : dto.getSpareCount();
            for (int i = 0; i < spareCount; i++) {
                FinishRoll spare = buildFinishRoll(order, roll, null, rowSort++, IS_SPARE_YES);
                spareRollNos.add(allocAndInsertFinish(spare));
                finishRollSourceBinder.bind(new FinishRollSourceBinder.BindRequest(
                        orderUuid, spare, roll.getUuid(), "配置预留备用号"));
            }
        }

        updateMixProcessFlag(orderUuid);
        FinishConfigSaveVO vo = new FinishConfigSaveVO();
        vo.setOrderUuid(orderUuid);
        vo.setOriginalUuid(rollUuid);
        vo.setFinishRollNos(finishRollNos);
        vo.setSpareRollNos(spareRollNos);
        return vo;
    }

    @Override
    public FinishPreviewVO previewRewindPlan(String orderUuid, String rollUuid, RewindPlanPreviewDTO dto) {
        FinishConfigQuantityValidator.requireWithinLimit(dto);
        ProcessOrder order = getById(orderUuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        OriginalRoll roll = originalRollMapper.selectById(rollUuid);
        if (roll == null || !orderUuid.equals(roll.getOrderUuid())) {
            throw new BusinessException(ErrorCode.E002, "原纸明细不存在");
        }
        validateRewindPreviewPlan(dto);
        validateSameSpecRewind(dto, roll);
        if (dto.getRewindMode() != null && dto.getRewindMode() == 5) {
            validateRewindSegmentSources(dto.getSegments(), orderRollMap(orderUuid));
        }
        return buildRewindPreview(orderUuid, roll, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(String uuid, Integer targetStatus, String reason) {
        businessLockService.lockProcessOrders(List.of(uuid));
        ProcessOrder order = getById(uuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        OrderStatus from = OrderStatus.of(order.getOrderStatus());
        OrderStatus to = OrderStatus.of(targetStatus);
        StateMachine.assertTransition(from, to);
        assertPublicStatusCommand(from, to);

        // 回退业务规则校验与数据清理
        if (isRollback(from, to)) {
            String rollbackReason = requireRollbackReason(reason);
            validateRollback(order, from, to);
            cleanupDataOnRollback(order, from, to);
            // 记录回退操作日志
            operationLogService.record(
                    OperationLogService.BIZ_TYPE_ORDER,
                    order.getUuid(),
                    order.getOrderNo(),
                    OperationLogService.ACTION_ROLLBACK,
                    currentOperator(),
                    String.format("状态回退：%s(%d)→%s(%d)，原因：%s",
                            from.getDesc(), from.getCode(),
                            to.getDesc(), to.getCode(),
                            rollbackReason)
            );
        }

        // 4→5 结算前置校验
        if (from == OrderStatus.FINISHED && to == OrderStatus.SETTLED) {
            if (order.getTotalAmount() == null || order.getTotalAmount().signum() == 0) {
                throw new BusinessException(ErrorCode.E003, "加工费未计算，不可结算");
            }
        }

        order.setOrderStatus(to.getCode());
        ConcurrencyGuard.requireUpdated(updateById(order));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSettled(String uuid) {
        businessLockService.lockProcessOrders(List.of(uuid));
        ProcessOrder order = getById(uuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        if (!Integer.valueOf(STATUS_FINISHED).equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.E003, "仅已完成加工单可结算");
        }
        if (order.getTotalAmount() == null || order.getTotalAmount().signum() <= 0) {
            throw new BusinessException(ErrorCode.E003, "加工费未计算，不可结算");
        }
        ConcurrencyGuard.requireRowUpdated(getBaseMapper().update(null,
                new LambdaUpdateWrapper<ProcessOrder>()
                        .eq(ProcessOrder::getUuid, uuid)
                        .eq(ProcessOrder::getOrderStatus, STATUS_FINISHED)
                        .set(ProcessOrder::getOrderStatus, STATUS_SETTLED)
                        .set(ProcessOrder::getUpdateBy, currentOperator())
                        .set(ProcessOrder::getUpdateTime, LocalDateTime.now())
                        .setSql("version = version + 1")));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeProcessing(String uuid, String reason) {
        changeStatus(uuid, OrderStatus.TO_RECORD.getCode(), reason);
    }

    /**
     * /status is retained for rollback compatibility and the single operational
     * completion transition. All other forward transitions have their own
     * business command (draft submit, issue/print, back-record, settlement).
     */
    private void assertPublicStatusCommand(OrderStatus from, OrderStatus to) {
        if (to == OrderStatus.TO_RECORD && from == OrderStatus.PROCESSING) {
            return;
        }
        if (isRollback(from, to)) {
            return;
        }
        throw new BusinessException(ErrorCode.E001,
                "请使用对应的业务命令执行状态流转");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackToDraft(String uuid, String reason) {
        businessLockService.lockProcessOrders(List.of(uuid));
        ProcessOrder order = getById(uuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        OrderStatus from = OrderStatus.of(order.getOrderStatus());
        String rollbackReason = requireRollbackReason(reason);
        validateDeepRollbackToDraft(order, from);
        cleanupBackRecordActuals(order);
        clearGeneratedProductionData(order);
        resetIssueAndBackRecordFields(order);
        resetCalculatedAmounts(order);
        updateOrderForDraftRollback(order);
        operationLogService.record(
                OperationLogService.BIZ_TYPE_ORDER,
                order.getUuid(),
                order.getOrderNo(),
                OperationLogService.ACTION_ROLLBACK,
                currentOperator(),
                "深度回退到草稿，清理下发、回录和产物数据，原因：" + rollbackReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidOrder(String uuid, ProcessOrderVoidDTO dto) {
        businessLockService.lockProcessOrders(List.of(uuid));
        ProcessOrder order = getById(uuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        validateVoidOrder(order);
        markOrderVoided(order, dto.getReason().trim());
    }

    private void validateVoidOrder(ProcessOrder order) {
        Integer status = order.getOrderStatus();
        if (status != null && status == STATUS_VOIDED) {
            throw new BusinessException(ErrorCode.E003, "加工单已作废");
        }
        if (status == null || !List.of(STATUS_DRAFT, STATUS_PENDING, STATUS_PROCESSING).contains(status)) {
            throw new BusinessException(ErrorCode.E003, "仅草稿、待下发或未回录的加工中加工单可作废");
        }
        ensureOrderNotReferencedBySettle(order.getUuid(), "加工单已被结算单引用，不可作废");
        ensureOrderHasNoDeliveryDetail(order.getUuid(), "加工单已有出库单明细引用，不可作废");
        ensureOrderHasNoOutboundFinish(order.getUuid());
        if (status == STATUS_PROCESSING) {
            ensureNoBackRecordData(order);
        }
    }

    private void markOrderVoided(ProcessOrder order, String reason) {
        LocalDateTime now = LocalDateTime.now();
        String operator = currentOperator();
        ConcurrencyGuard.requireRowUpdated(getBaseMapper().update(null,
                new LambdaUpdateWrapper<ProcessOrder>()
                        .eq(ProcessOrder::getUuid, order.getUuid())
                        .in(ProcessOrder::getOrderStatus, List.of(STATUS_DRAFT, STATUS_PENDING, STATUS_PROCESSING))
                        .set(ProcessOrder::getOrderStatus, STATUS_VOIDED)
                        .set(ProcessOrder::getVoidTime, now)
                        .set(ProcessOrder::getVoidUser, operator)
                        .set(ProcessOrder::getVoidReason, reason)
                        .set(ProcessOrder::getUpdateTime, now)
                        .set(ProcessOrder::getUpdateBy, operator)
                        .setSql("version = version + 1")));
        operationLogService.record(OperationLogService.BIZ_TYPE_ORDER, order.getUuid(), order.getOrderNo(),
                OperationLogService.ACTION_VOID_ORDER, operator, reason);
    }

    private void ensureOrderNotReferencedBySettle(String orderUuid, String message) {
        Long settleCount = settleDetailMapper.selectCount(
                new LambdaQueryWrapper<com.paper.mes.settle.entity.SettleDetail>()
                        .eq(com.paper.mes.settle.entity.SettleDetail::getOrderUuid, orderUuid));
        if (settleCount > 0) {
            throw new BusinessException(ErrorCode.E003, message);
        }
    }

    private void ensureOrderHasNoOutboundFinish(String orderUuid) {
        Long outCount = finishRollMapper.selectCount(
                new LambdaQueryWrapper<FinishRoll>()
                        .eq(FinishRoll::getOrderUuid, orderUuid)
                        .eq(FinishRoll::getFinishStatus, FINISH_STATUS_OUT));
        if (outCount > 0) {
            throw new BusinessException(ErrorCode.E003, "已有成品出库，不可作废");
        }
    }

    private void ensureOrderHasNoDeliveryDetail(String orderUuid, String message) {
        Long count = deliveryDetailMapper.selectCount(new LambdaQueryWrapper<DeliveryDetail>()
                .eq(DeliveryDetail::getIsDeleted, 0)
                .eq(DeliveryDetail::getOrderUuid, orderUuid));
        if (count > 0) {
            throw new BusinessException(ErrorCode.E003, message);
        }
    }

    private void ensureNoBackRecordData(ProcessOrder order) {
        if (order.getBackRecordTime() != null || StringUtils.hasText(order.getBackRecordUser())
                || StringUtils.hasText(order.getSnapFinish())) {
            throw new BusinessException(ErrorCode.E003, "加工单已有回录数据，不可执行该操作");
        }
        Long actualCount = finishRollMapper.selectCount(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, order.getUuid())
                .isNotNull(FinishRoll::getActualWeight));
        if (actualCount > 0) {
            throw new BusinessException(ErrorCode.E003, "加工单已有成品实重，不可执行该操作");
        }
    }

    private String requireRollbackReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ErrorCode.E001, "回退原因不能为空");
        }
        return reason.trim();
    }

    /**
     * 判断是否为回退操作
     */
    private boolean isRollback(OrderStatus from, OrderStatus to) {
        return (from == OrderStatus.PENDING && to == OrderStatus.DRAFT)
                || (from == OrderStatus.PROCESSING && to == OrderStatus.PENDING)
                || (from == OrderStatus.TO_RECORD && to == OrderStatus.PENDING)
                || (from == OrderStatus.FINISHED && to == OrderStatus.TO_RECORD);
    }

    /**
     * 回退前置校验：检查未结算、未出库
     */
    private void validateRollback(ProcessOrder order, OrderStatus from, OrderStatus to) {
        // 1. 检查是否已结算
        if (order.getOrderStatus() >= OrderStatus.SETTLED.getCode()) {
            throw new BusinessException(ErrorCode.E003, "加工单已结算，不可回退");
        }

        ensureOrderNotReferencedBySettle(order.getUuid(), "加工单已被结算单引用，不可回退");
        ensureOrderHasNoDeliveryDetail(order.getUuid(), "加工单已有出库单明细引用，不可回退");

        // 2. 检查是否有成品已出库
        Long outCount = finishRollMapper.selectCount(
                new LambdaQueryWrapper<FinishRoll>()
                        .eq(FinishRoll::getOrderUuid, order.getUuid())
                        .eq(FinishRoll::getFinishStatus, FINISH_STATUS_OUT)
        );
        if (outCount > 0) {
            throw new BusinessException(ErrorCode.E003, "已有成品出库，不可回退");
        }
        if ((from == OrderStatus.PENDING && to == OrderStatus.DRAFT)
                || (from == OrderStatus.PROCESSING && to == OrderStatus.PENDING)) {
            ensureNoBackRecordData(order);
        }
    }

    /**
     * 回退数据清理：清空快照、回录信息、重置成品状态
     */
    private void cleanupDataOnRollback(ProcessOrder order, OrderStatus from, OrderStatus to) {
        if (from == OrderStatus.PENDING && to == OrderStatus.DRAFT) {
            clearGeneratedProductionData(order);
            resetIssueAndBackRecordFields(order);
            resetCalculatedAmounts(order);
        }

        if (from == OrderStatus.PROCESSING && to == OrderStatus.PENDING) {
            resetIssueAndBackRecordFields(order);
        }

        if (from == OrderStatus.FINISHED && to == OrderStatus.TO_RECORD) {
            // 4→3：清理完成快照和回录信息（保留打印快照snap_print）
            order.setSnapFinish(null);
            order.setBackRecordTime(null);
            order.setBackRecordUser(null);
            backRecordReopenService.reopen(order.getUuid(), currentOperator());
            // 计费数据保留，以便重新回录时对比
            // 成品状态保持已入库(2)：成品已真实产出，只是回录数据需修正
        }

        if (from == OrderStatus.TO_RECORD && to == OrderStatus.PENDING) {
            // 3→1：清理完成快照、回录信息
            resetIssueAndBackRecordFields(order);
            cleanupBackRecordActuals(order);

            // 成品回退到待入库状态（已入库→待入库）
            finishRollMapper.update(null,
                    new LambdaUpdateWrapper<FinishRoll>()
                            .eq(FinishRoll::getOrderUuid, order.getUuid())
                            .eq(FinishRoll::getFinishStatus, 2)  // 已入库
                            .set(FinishRoll::getFinishStatus, 1)  // → 待入库
            );
        }
    }

    private void validateDeepRollbackToDraft(ProcessOrder order, OrderStatus from) {
        if (from == OrderStatus.DRAFT) {
            throw new BusinessException(ErrorCode.E003, "加工单已处于草稿状态，无需重复回退");
        }
        if (from == OrderStatus.SETTLED || from == OrderStatus.VOIDED) {
            throw new BusinessException(ErrorCode.E003, "已结算或已作废加工单不可回退编辑");
        }
        ensureOrderNotReferencedBySettle(order.getUuid(), "加工单已被结算单引用，不可回退编辑");
        ensureOrderHasNoDeliveryDetail(order.getUuid(), "加工单已有出库单明细引用，不可回退编辑");
        ensureOrderHasNoOutboundFinish(order.getUuid());
    }

    private void cleanupBackRecordActuals(ProcessOrder order) {
        String orderUuid = order.getUuid();
        clearRollActuals(orderUuid);
        voidDirectShipFinishes(orderUuid);
        clearProcessFinishActuals(orderUuid);
        clearStepLosses(orderUuid);
        clearStageOutputActuals(orderUuid);
    }

    private void clearRollActuals(String orderUuid) {
        originalRollMapper.update(null, new LambdaUpdateWrapper<OriginalRoll>()
                .eq(OriginalRoll::getOrderUuid, orderUuid)
                .set(OriginalRoll::getActualGramWeight, null)
                .set(OriginalRoll::getActualWidth, null)
                .set(OriginalRoll::getActualWeight, null)
                .set(OriginalRoll::getTotalLossWeight, null)
                .set(OriginalRoll::getTotalLossRatio, ZERO_AMOUNT)
                .set(OriginalRoll::getIsChecked, 0)
                .set(OriginalRoll::getCheckUser, null)
                .set(OriginalRoll::getCheckTime, null)
                .set(OriginalRoll::getRollStatus, ROLL_STATUS_PENDING));
    }

    private void voidDirectShipFinishes(String orderUuid) {
        finishRollMapper.update(null, new LambdaUpdateWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, orderUuid)
                .eq(FinishRoll::getSourceType, SOURCE_DIRECT_SHIP)
                .set(FinishRoll::getRollNoStatus, ROLL_NO_VOID));
    }

    private void clearProcessFinishActuals(String orderUuid) {
        finishRollMapper.update(null, new LambdaUpdateWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, orderUuid)
                .and(w -> w.isNull(FinishRoll::getSourceType).or().ne(FinishRoll::getSourceType, SOURCE_DIRECT_SHIP))
                .set(FinishRoll::getActualWeight, null)
                .set(FinishRoll::getRemainingWeight, null)
                .set(FinishRoll::getScrapWeight, null)
                .set(FinishRoll::getIsAbnormal, 0)
                .set(FinishRoll::getAbnormalType, null)
                .set(FinishRoll::getActualRemark, null)
                .set(FinishRoll::getFinishStatus, FINISH_STATUS_PENDING));
    }

    private void clearStepLosses(String orderUuid) {
        processStepMapper.update(null, new LambdaUpdateWrapper<ProcessStep>()
                .eq(ProcessStep::getOrderUuid, orderUuid)
                .set(ProcessStep::getLossWeight, null));
    }

    private void clearStageOutputActuals(String orderUuid) {
        processStageOutputMapper.update(null, new LambdaUpdateWrapper<ProcessStageOutput>()
                .eq(ProcessStageOutput::getOrderUuid, orderUuid)
                .set(ProcessStageOutput::getActualWeight, null));
    }

    private void clearGeneratedProductionData(ProcessOrder order) {
        String orderUuid = order.getUuid();
        List<FinishRoll> finishes = finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, orderUuid)
                .and(w -> w.isNull(FinishRoll::getRollNoStatus)
                        .or()
                        .ne(FinishRoll::getRollNoStatus, ROLL_NO_VOID)));
        if (!finishes.isEmpty()) {
            finishOriginalRelMapper.delete(new LambdaQueryWrapper<FinishOriginalRel>()
                    .eq(FinishOriginalRel::getOrderUuid, orderUuid));
        }
        for (FinishRoll finish : finishes) {
            finish.setRollNoStatus(ROLL_NO_VOID);
            finishRollMapper.updateById(finish);
        }
        processStageInputRelMapper.delete(new LambdaQueryWrapper<ProcessStageInputRel>()
                .eq(ProcessStageInputRel::getOrderUuid, orderUuid));
        processStageOutputMapper.delete(new LambdaQueryWrapper<ProcessStageOutput>()
                .eq(ProcessStageOutput::getOrderUuid, orderUuid));
        processParamMapper.delete(new LambdaQueryWrapper<ProcessParam>()
                .eq(ProcessParam::getOrderUuid, orderUuid));
        processStepMapper.delete(new LambdaQueryWrapper<ProcessStep>()
                .eq(ProcessStep::getOrderUuid, orderUuid));
        originalRollMapper.update(null, new LambdaUpdateWrapper<OriginalRoll>()
                .eq(OriginalRoll::getOrderUuid, orderUuid)
                .set(OriginalRoll::getRollStatus, ROLL_STATUS_PENDING)
                .set(OriginalRoll::getProcessAmount, ZERO_AMOUNT));
    }

    private void resetIssueAndBackRecordFields(ProcessOrder order) {
        order.setSnapPrint(null);
        order.setSnapFinish(null);
        order.setPrintStatus(PRINT_STATUS_UNPRINTED);
        order.setPrintCount(0);
        order.setLastPrintTime(null);
        order.setLastPrintUser(null);
        order.setBackRecordTime(null);
        order.setBackRecordUser(null);
    }

    private void resetCalculatedAmounts(ProcessOrder order) {
        order.setProcessAmountNoTax(null);
        order.setProcessAmountTax(null);
        order.setExtraAmountNoTax(null);
        order.setExtraAmountTax(null);
        order.setTotalAmountNoTax(null);
        order.setTotalAmountTax(null);
        order.setTotalProcessAmount(null);
        order.setTotalExtraAmount(null);
        order.setTotalAmount(null);
        order.setTotalFinishWeight(null);
        order.setTotalStepCount(0);
        order.setHasExtraStep(0);
        order.setActualTotalKnife(null);
        order.setIsMixProcess(0);
    }

    private void updateOrderForDraftRollback(ProcessOrder order) {
        ConcurrencyGuard.requireRowUpdated(getBaseMapper().update(null,
                new LambdaUpdateWrapper<ProcessOrder>()
                        .eq(ProcessOrder::getUuid, order.getUuid())
                        .set(ProcessOrder::getOrderStatus, STATUS_DRAFT)
                        .set(ProcessOrder::getSnapPrint, null)
                        .set(ProcessOrder::getSnapFinish, null)
                        .set(ProcessOrder::getPrintStatus, PRINT_STATUS_UNPRINTED)
                        .set(ProcessOrder::getPrintCount, 0)
                        .set(ProcessOrder::getLastPrintTime, null)
                        .set(ProcessOrder::getLastPrintUser, null)
                        .set(ProcessOrder::getBackRecordTime, null)
                        .set(ProcessOrder::getBackRecordUser, null)
                        .set(ProcessOrder::getProcessAmountNoTax, null)
                        .set(ProcessOrder::getProcessAmountTax, null)
                        .set(ProcessOrder::getExtraAmountNoTax, null)
                        .set(ProcessOrder::getExtraAmountTax, null)
                        .set(ProcessOrder::getTotalAmountNoTax, null)
                        .set(ProcessOrder::getTotalAmountTax, null)
                        .set(ProcessOrder::getTotalProcessAmount, null)
                        .set(ProcessOrder::getTotalExtraAmount, null)
                        .set(ProcessOrder::getTotalAmount, null)
                        .set(ProcessOrder::getTotalFinishWeight, null)
                        .set(ProcessOrder::getTotalStepCount, 0)
                        .set(ProcessOrder::getHasExtraStep, 0)
                        .set(ProcessOrder::getActualTotalKnife, null)
                        .set(ProcessOrder::getIsMixProcess, 0)
                        .set(ProcessOrder::getUpdateBy, currentOperator())
                        .set(ProcessOrder::getUpdateTime, LocalDateTime.now())
                        .setSql("version = version + 1")));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeRollStatus(String rollUuid, Integer targetStatus) {
        LockedRoll locked = lockRollAndOrder(rollUuid);
        validateRollProductionEditable(locked.order());
        OriginalRoll roll = locked.roll();
        RollStatus from = RollStatus.of(roll.getRollStatus());
        RollStatus to = RollStatus.of(targetStatus);
        StateMachine.assertTransition(from, to);
        roll.setRollStatus(to.getCode());
        ConcurrencyGuard.requireRowUpdated(originalRollMapper.updateById(roll));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrintResultVO issue(String uuid) {
        businessLockService.lockProcessOrders(List.of(uuid));
        ProcessOrder order = getById(uuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        List<OriginalRoll> rolls = originalRollMapper.selectList(
                new LambdaQueryWrapper<OriginalRoll>()
                        .eq(OriginalRoll::getOrderUuid, uuid)
                        .orderByAsc(OriginalRoll::getRowSort));
        List<FinishRoll> finishRolls = finishRollMapper.selectList(
                new LambdaQueryWrapper<FinishRoll>()
                        .eq(FinishRoll::getOrderUuid, uuid)
                        .ne(FinishRoll::getRollNoStatus, ROLL_NO_VOID)
                        .orderByAsc(FinishRoll::getRowSort));
        finishRolls = ProcessOrderPrintFinishPolicy.printable(finishRolls);
        List<ProcessStep> steps = processStepMapper.selectList(
                new LambdaQueryWrapper<ProcessStep>()
                        .eq(ProcessStep::getOrderUuid, uuid)
                        .orderByAsc(ProcessStep::getOriginalUuid)
                        .orderByAsc(ProcessStep::getStepSort));
        List<ProcessParam> processParams = processParamMapper.selectList(
                new LambdaQueryWrapper<ProcessParam>()
                        .eq(ProcessParam::getOrderUuid, uuid)
                        .orderByAsc(ProcessParam::getOriginalUuid)
                        .orderByAsc(ProcessParam::getLayerSort));
        List<FinishOriginalRel> finishOriginalRels = finishOriginalRelMapper.selectList(
                new LambdaQueryWrapper<FinishOriginalRel>()
                        .eq(FinishOriginalRel::getOrderUuid, uuid));

        if (order.getOrderStatus() != STATUS_PENDING) {
            throw new BusinessException("仅待下发状态可执行下发");
        }
        ProcessOrderPrintableConfigValidator.validate(rolls, finishRolls, steps, finishOriginalRels);
        StateMachine.assertTransition(OrderStatus.of(order.getOrderStatus()), OrderStatus.PROCESSING);
        LocalDateTime now = LocalDateTime.now();
        String printUser = currentOperator();
        order.setSnapPrint(buildSnapPrint(order, rolls, finishRolls, steps, processParams, finishOriginalRels,
                0, now, null, printUser));
        order.setOrderStatus(OrderStatus.PROCESSING.getCode());
        order.setPrintStatus(PRINT_STATUS_UNPRINTED);
        order.setPrintCount(0);
        ConcurrencyGuard.requireUpdated(updateById(order));
        return buildPrintResult(order, finishRolls, 0, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrintResultVO print(String uuid, PrintDTO dto) {
        businessLockService.lockProcessOrders(List.of(uuid));
        ProcessOrder order = getById(uuid);
        if (order == null) throw new BusinessException(ErrorCode.E002, "加工单不存在");
        if (order.getOrderStatus() != OrderStatus.PROCESSING.getCode()) {
            throw new BusinessException("当前状态不允许打印");
        }
        boolean firstPrint = order.getPrintCount() == null || order.getPrintCount() == 0;
        if (!firstPrint && !StringUtils.hasText(dto.getReason())) {
            throw new BusinessException("补打必须填写原因");
        }
        List<FinishRoll> finishRolls = finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, uuid)
                .ne(FinishRoll::getRollNoStatus, ROLL_NO_VOID)
                .orderByAsc(FinishRoll::getRowSort));
        finishRolls = ProcessOrderPrintFinishPolicy.printable(finishRolls);
        int nextCount = (order.getPrintCount() == null ? 0 : order.getPrintCount()) + 1;
        LocalDateTime now = LocalDateTime.now();
        String printUser = currentOperator();
        order.setPrintStatus(PRINT_STATUS_PRINTED);
        order.setPrintCount(nextCount);
        order.setLastPrintTime(now);
        order.setLastPrintUser(printUser);
        ConcurrencyGuard.requireUpdated(updateById(order));
        if (!firstPrint) {
            operationLogService.record(OperationLogService.BIZ_TYPE_ORDER, order.getUuid(), order.getOrderNo(),
                    OperationLogService.ACTION_REPRINT, printUser, dto.getReason().trim());
        }
        return buildPrintResult(order, finishRolls, nextCount, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrintResultVO physicalReprint(String uuid, PhysicalReprintDTO dto) {
        businessLockService.lockProcessOrders(List.of(uuid));
        ProcessOrder order = requireOrder(uuid);
        requirePhysicalReprintAllowed(order, dto);
        List<FinishRoll> finishRolls = finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, uuid)
                .ne(FinishRoll::getRollNoStatus, ROLL_NO_VOID)
                .orderByAsc(FinishRoll::getRowSort));
        finishRolls = ProcessOrderPrintFinishPolicy.printable(finishRolls);
        int nextCount = (order.getPrintCount() == null ? 0 : order.getPrintCount()) + 1;
        LocalDateTime now = LocalDateTime.now();
        String operator = currentOperator();
        Integer originalStatus = order.getOrderStatus();
        order.setPrintStatus(PRINT_STATUS_PRINTED);
        order.setPrintCount(nextCount);
        order.setLastPrintTime(now);
        order.setLastPrintUser(operator);
        ConcurrencyGuard.requireUpdated(updateById(order));
        operationLogService.record(OperationLogService.BIZ_TYPE_ORDER, order.getUuid(), order.getOrderNo(),
                OperationLogService.ACTION_REPRINT, operator, physicalReprintRemark(dto));
        order.setOrderStatus(originalStatus);
        return buildPrintResult(order, finishRolls, nextCount, now);
    }

    private void requirePhysicalReprintAllowed(ProcessOrder order, PhysicalReprintDTO dto) {
        Integer status = order.getOrderStatus();
        if (status == null || status < STATUS_PROCESSING || status > STATUS_SETTLED) {
            throw new BusinessException(ErrorCode.E004, "当前加工单状态不允许物理补打");
        }
        String snapshot = dto.getVersion() == PrintViewVersion.FINISHED
                ? order.getSnapFinish() : order.getSnapPrint();
        if (!StringUtils.hasText(snapshot)) {
            throw new BusinessException(ErrorCode.E002, "所选打印版本快照不存在");
        }
    }

    private String physicalReprintRemark(PhysicalReprintDTO dto) {
        String versionName = dto.getVersion() == PrintViewVersion.FINISHED ? "完工版本" : "下发版本";
        return versionName + "：" + dto.getReason().trim();
    }

    /**
     * 构建下发快照 JSON 字符串。根节点强制写入 schema_version，
     * 锁定原纸标称参数与预生成成品卷号，作为与车间纸质单据对账的不可变基准。
     */
    private String buildSnapPrint(ProcessOrder order, List<OriginalRoll> rolls, List<FinishRoll> finishRolls,
                                  List<ProcessStep> steps, List<ProcessParam> processParams,
                                  List<FinishOriginalRel> finishOriginalRels, int printCount,
                                  LocalDateTime printTime, String reason, String printUser) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("schema_version", SNAP_SCHEMA_VERSION);
        snap.put("order_no", order.getOrderNo());
        snap.put("customer_name", order.getCustomerName());
        snap.put("print_count", printCount);
        snap.put("print_time", printTime.toString());
        snap.put("print_user", printUser);
        if (StringUtils.hasText(reason)) {
            snap.put("reprint_reason", reason);
        }

        Map<String, List<ProcessStep>> stepsByRoll = groupStepsByRoll(steps);
        Map<String, List<ProcessParam>> paramsByRoll = groupParamsByRoll(processParams);
        Map<String, List<FinishOriginalRel>> relsByFinish = groupRelsByFinish(finishOriginalRels);

        List<Map<String, Object>> rollSnaps = new ArrayList<>(rolls.size());
        for (OriginalRoll r : rolls) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("uuid", r.getUuid());
            m.put("row_sort", r.getRowSort());
            m.put("roll_no", r.getRollNo());
            m.put("paper_name", r.getPaperName());
            m.put("gram_weight", r.getGramWeight());
            m.put("original_width", r.getOriginalWidth());
            m.put("original_diameter", r.getOriginalDiameter());
            m.put("core_diameter", r.getCoreDiameter());
            m.put("roll_weight", r.getRollWeight());
            m.put("piece_num", r.getPieceNum());
            m.put("process_mode", r.getProcessMode());
            m.put("main_step_type", r.getMainStepType());
            m.put("steps", stepSnaps(stepsByRoll.get(r.getUuid())));
            m.put("rewind_params", processParamSnaps(paramsByRoll.get(r.getUuid())));
            rollSnaps.add(m);
        }
        snap.put("original_rolls", rollSnaps);

        List<Map<String, Object>> finishSnaps = new ArrayList<>(finishRolls.size());
        for (FinishRoll f : finishRolls) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("uuid", f.getUuid());
            m.put("finish_roll_no", f.getFinishRollNo());
            m.put("is_spare", f.getIsSpare());
            m.put("is_remain", f.getIsRemain());
            m.put("paper_name", f.getPaperName());
            m.put("finish_width", f.getFinishWidth());
            m.put("finish_diameter", f.getFinishDiameter());
            m.put("finish_core_diameter", f.getFinishCoreDiameter());
            m.put("estimate_weight", f.getEstimateWeight());
            m.put("original_roll_nos", f.getOriginalRollNos());
            m.put("source_relations", finishRelSnaps(relsByFinish.get(f.getUuid())));
            finishSnaps.add(m);
        }
        snap.put("finish_rolls", finishSnaps);
        ProcessOrderSnapshotDetailCodec.append(
                snap, ProcessOrderPrintDetailPolicy.filter(snapshotDetail(order)), objectMapper);

        String json;
        try {
            json = objectMapper.writeValueAsString(snap);
        } catch (JsonProcessingException e) {
            throw new BusinessException("下发快照序列化失败");
        }
        // 验收兜底：缺 schema_version 拒绝写入。
        if (!json.contains("\"schema_version\"")) {
            throw new BusinessException("快照缺少 schema_version，拒绝写入");
        }
        return json;
    }

    private Map<String, List<ProcessStep>> groupStepsByRoll(List<ProcessStep> steps) {
        Map<String, List<ProcessStep>> grouped = new LinkedHashMap<>();
        for (ProcessStep step : steps) {
            grouped.computeIfAbsent(step.getOriginalUuid(), key -> new ArrayList<>()).add(step);
        }
        return grouped;
    }

    private Map<String, List<ProcessParam>> groupParamsByRoll(List<ProcessParam> params) {
        Map<String, List<ProcessParam>> grouped = new LinkedHashMap<>();
        for (ProcessParam param : params) {
            grouped.computeIfAbsent(param.getOriginalUuid(), key -> new ArrayList<>()).add(param);
        }
        return grouped;
    }

    private Map<String, List<FinishOriginalRel>> groupRelsByFinish(List<FinishOriginalRel> rels) {
        Map<String, List<FinishOriginalRel>> grouped = new LinkedHashMap<>();
        for (FinishOriginalRel rel : rels) {
            grouped.computeIfAbsent(rel.getFinishUuid(), key -> new ArrayList<>()).add(rel);
        }
        return grouped;
    }

    private List<Map<String, Object>> stepSnaps(List<ProcessStep> steps) {
        List<Map<String, Object>> snaps = new ArrayList<>();
        if (steps == null) {
            return snaps;
        }
        for (ProcessStep step : steps) {
            Map<String, Object> snap = new LinkedHashMap<>();
            snap.put("uuid", step.getUuid());
            snap.put("step_sort", step.getStepSort());
            snap.put("step_type", step.getStepType());
            snap.put("step_name", step.getStepName());
            snap.put("is_main", step.getIsMain());
            snap.put("knife_count", step.getKnifeCount());
            snap.put("process_weight", step.getProcessWeight());
            snap.put("unit_price", step.getUnitPrice());
            snap.put("billing_unit_price", step.getBillingUnitPrice());
            snap.put("step_amount", step.getStepAmount());
            snap.put("billing_mode", step.getBillingMode());
            snap.put("standard_quantity", step.getStandardQuantity());
            snap.put("billing_quantity", step.getBillingQuantity());
            snap.put("billing_amount", step.getBillingAmount());
            snap.put("standard_step_amount", step.getStandardStepAmount());
            snap.put("pricing_adjustment_amount", step.getPricingAdjustmentAmount());
            snap.put("pricing_adjustment_reason", step.getPricingAdjustmentReason());
            snap.put("pricing_adjusted_by", step.getPricingAdjustedBy());
            snap.put("pricing_adjusted_at", step.getPricingAdjustedAt());
            snap.put("remark", step.getRemark());
            snaps.add(snap);
        }
        return snaps;
    }

    private List<Map<String, Object>> processParamSnaps(List<ProcessParam> params) {
        List<Map<String, Object>> snaps = new ArrayList<>();
        if (params == null) {
            return snaps;
        }
        for (ProcessParam param : params) {
            Map<String, Object> snap = new LinkedHashMap<>();
            snap.put("uuid", param.getUuid());
            snap.put("step_uuid", param.getStepUuid());
            snap.put("param_mode", param.getParamMode());
            snap.put("layer_sort", param.getLayerSort());
            snap.put("out_diameter", param.getOutDiameter());
            snap.put("core_diameter", param.getCoreDiameter());
            snap.put("layer_width", param.getLayerWidth());
            snap.put("area_value", param.getAreaValue());
            snap.put("area_ratio", param.getAreaRatio());
            snap.put("split_ratio", param.getSplitRatio());
            snap.put("param_json", param.getParamJson());
            snaps.add(snap);
        }
        return snaps;
    }

    private List<Map<String, Object>> finishRelSnaps(List<FinishOriginalRel> rels) {
        List<Map<String, Object>> snaps = new ArrayList<>();
        if (rels == null) {
            return snaps;
        }
        for (FinishOriginalRel rel : rels) {
            Map<String, Object> snap = new LinkedHashMap<>();
            snap.put("uuid", rel.getUuid());
            snap.put("original_uuid", rel.getOriginalUuid());
            snap.put("share_ratio", rel.getShareRatio());
            snap.put("share_weight", rel.getShareWeight());
            snap.put("remark", rel.getRemark());
            snaps.add(snap);
        }
        return snaps;
    }

    private PrintResultVO buildPrintResult(ProcessOrder order, List<FinishRoll> finishRolls,
                                           int printCount, LocalDateTime printTime) {
        PrintResultVO vo = new PrintResultVO();
        vo.setOrderUuid(order.getUuid());
        vo.setOrderNo(order.getOrderNo());
        vo.setPrintCount(printCount);
        vo.setPrintStatus(order.getPrintStatus());
        vo.setReprint(printCount > 1);
        vo.setPrintTime(printTime);
        vo.setOrderStatus(order.getOrderStatus());
        List<String> formal = new ArrayList<>();
        List<String> spare = new ArrayList<>();
        for (FinishRoll f : finishRolls) {
            if (f.getFinishRollNo() == null) {
                continue;
            }
            if (IS_REMAIN_YES == (f.getIsRemain() == null ? 0 : f.getIsRemain())) {
                continue;
            }
            int spareFlag = f.getIsSpare() == null ? IS_SPARE_NO : f.getIsSpare();
            if (spareFlag == IS_SPARE_YES) {
                spare.add(f.getFinishRollNo());
            } else if (isFormalFinishRoll(f)) {
                formal.add(f.getFinishRollNo());
            }
        }
        vo.setFinishRollNos(formal);
        vo.setSpareRollNos(spare);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BackRecordResultVO backRecord(String uuid, BackRecordDTO dto) {
        businessLockService.lockProcessOrders(List.of(uuid));
        ProcessOrder order = requireBackRecordOrder(uuid, dto.getExpectedVersion());
        BackRecordWarehousePolicy.WarehouseSnapshot warehouse =
                backRecordWarehousePolicy.requireEnabled(dto.getWarehouseUuid());
        requireConsistentBackRecordWarehouse(order, warehouse.uuid());
        order.setWarehouseUuid(warehouse.uuid());

        List<OriginalRoll> rolls = originalRollMapper.selectList(
                new LambdaQueryWrapper<OriginalRoll>()
                        .eq(OriginalRoll::getOrderUuid, uuid)
                        .orderByAsc(OriginalRoll::getRowSort));
        List<FinishRoll> finishRolls = finishRollMapper.selectList(
                new LambdaQueryWrapper<FinishRoll>()
                        .eq(FinishRoll::getOrderUuid, uuid)
                        .orderByAsc(FinishRoll::getRowSort));
        businessLockService.lockFinishRolls(finishRolls.stream().map(FinishRoll::getUuid).toList());
        finishRolls = finishRollMapper.selectList(
                new LambdaQueryWrapper<FinishRoll>()
                        .eq(FinishRoll::getOrderUuid, uuid)
                        .orderByAsc(FinishRoll::getRowSort));
        List<ProcessStep> steps = processStepMapper.selectList(
                new LambdaQueryWrapper<ProcessStep>()
                        .eq(ProcessStep::getOrderUuid, uuid));
        List<FinishOriginalRel> finishOriginalRels = finishOriginalRelMapper.selectList(
                new LambdaQueryWrapper<FinishOriginalRel>()
                        .eq(FinishOriginalRel::getOrderUuid, uuid));
        BackRecordScope scope = backRecordScopeResolver.resolve(
                rolls, finishRolls, steps, finishOriginalRels, dto);
        Map<String, OriginalRoll> scopeRolls = scope.rolls().stream().collect(
                java.util.stream.Collectors.toMap(OriginalRoll::getUuid, roll -> roll));

        writeRollActuals(dto.getRolls(), scopeRolls);
        BackRecordOnSiteFinishRecorder.Result onSiteResult = backRecordOnSiteFinishRecorder.record(
                dto.getFinishes(), new BackRecordOnSiteFinishRecorder.Context(
                        order, scope.rolls(), scope.finishes(), scope.relations()));
        scope.finishes().addAll(onSiteResult.finishes());
        scope.relations().addAll(onSiteResult.relations());
        backRecordFinishRecorder.record(
                standardFinishDtos(dto.getFinishes(), onSiteResult.managedExistingUuids()),
                new BackRecordFinishRecorder.Context(
                        standardFinishRolls(scope.finishes(), onSiteResult.managedExistingUuids()),
                        scope.rolls(), scope.relations()));
        BackRecordOnSiteTrimRecorder.Result trimResult = backRecordOnSiteTrimRecorder.record(
                dto.getTrims(), new BackRecordOnSiteTrimRecorder.Context(
                        order, scope.rolls(), scope.finishes()));
        scope.finishes().addAll(trimResult.finishes());
        scope.relations().addAll(trimResult.relations());
        refreshBackRecordRelations(order.getUuid(), scope);
        writeStepLosses(dto.getSteps(), scope.steps());
        validateOnSiteActualSteps(scope.rolls(), scope.steps(), dto.getSteps());
        updateRollLossSummaries(scope.rolls(), scope.steps(), scope.finishes(), scope.relations());

        int directShipGenerated = generateDirectShipFinishes(order, scope.rolls(), scope.finishes());
        List<BackRecordResultVO.RollCheck> rollChecks = computeClosureChecks(
                order, scope.rolls(), scope.finishes(), scope.steps(), scope.relations());
        BackRecordResultVO.RollCheck blockCheck = firstBlockCheck(rollChecks);
        if (blockCheck != null) {
            authorizeBlockRelease(dto, order, blockCheck);
        } else {
            confirmWarnVariance(dto, order, firstWarnCheck(rollChecks));
        }

        int voided = finishProcessRollsAndVoidSpare(scope.finishes());
        assignBackRecordWarehouse(scope.finishes(), warehouse.uuid());
        LocalDateTime now = LocalDateTime.now();
        String backRecordUser = currentOperator();
        markFirstStockInTime(scope.finishes(), now);
        markRollsRecorded(scope.rolls(), backRecordUser, now);
        boolean completeOrder = Boolean.TRUE.equals(dto.getCompleteOrder());
        if (completeOrder) {
            requireAllRollsRecorded(rolls);
            rollChecks = completeBackRecordOrder(order, rolls, steps, now, backRecordUser, warehouse);
        } else {
            ConcurrencyGuard.requireUpdated(updateById(order));
        }

        operationLogService.record(BIZ_TYPE_ORDER, order.getUuid(), order.getOrderNo(),
                OperationLogService.ACTION_BACK_RECORD, backRecordUser,
                completeOrder ? null : "部分回录：" + scope.rolls().size() + " 卷");

        return buildBackRecordResult(order, now, rollChecks, directShipGenerated, voided,
                scope.rolls().size(), remainingRollCount(rolls));
    }

    private ProcessOrder requireBackRecordOrder(String uuid, Integer expectedVersion) {
        ProcessOrder order = getById(uuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        if (order.getOrderStatus() == null || order.getOrderStatus() != STATUS_TO_RECORD) {
            throw new BusinessException(ErrorCode.E001, "仅待回录状态可提交回录");
        }
        if (expectedVersion == null || !Objects.equals(order.getVersion(), expectedVersion)) {
            throw new BusinessException(ErrorCode.E006, "加工单已被其他页面修改，请刷新后重试");
        }
        return order;
    }

    private void refreshBackRecordRelations(String orderUuid, BackRecordScope scope) {
        Set<String> rollIds = scope.rolls().stream().map(OriginalRoll::getUuid)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> finishIds = scope.finishes().stream().map(FinishRoll::getUuid)
                .collect(java.util.stream.Collectors.toSet());
        List<FinishOriginalRel> current = finishOriginalRelMapper.selectList(
                new LambdaQueryWrapper<FinishOriginalRel>()
                        .eq(FinishOriginalRel::getOrderUuid, orderUuid));
        scope.relations().clear();
        current.stream()
                .filter(relation -> rollIds.contains(relation.getOriginalUuid()))
                .filter(relation -> finishIds.contains(relation.getFinishUuid()))
                .forEach(scope.relations()::add);
    }

    private void requireConsistentBackRecordWarehouse(ProcessOrder order, String warehouseUuid) {
        if (StringUtils.hasText(order.getWarehouseUuid())
                && !Objects.equals(order.getWarehouseUuid(), warehouseUuid)) {
            throw new BusinessException(ErrorCode.E003, "部分回录必须使用加工单已确定的同一入库仓库");
        }
    }

    private List<BackRecordResultVO.RollCheck> completeBackRecordOrder(
            ProcessOrder order, List<OriginalRoll> rolls, List<ProcessStep> steps,
            LocalDateTime now, String operator,
            BackRecordWarehousePolicy.WarehouseSnapshot warehouse) {
        StateMachine.assertTransition(OrderStatus.of(order.getOrderStatus()), OrderStatus.FINISHED);
        List<FinishRoll> finishes = finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, order.getUuid()).orderByAsc(FinishRoll::getRowSort));
        List<FinishOriginalRel> relations = finishOriginalRelMapper.selectList(
                new LambdaQueryWrapper<FinishOriginalRel>()
                        .eq(FinishOriginalRel::getOrderUuid, order.getUuid()));
        List<BackRecordResultVO.RollCheck> checks = computeClosureChecks(
                order, rolls, finishes, steps, relations);
        order.setOrderStatus(STATUS_FINISHED);
        order.setBackRecordTime(now);
        order.setBackRecordUser(operator);
        order.setSnapFinish(buildSnapFinish(order, rolls, finishes, now, checks, warehouse));
        ConcurrencyGuard.requireUpdated(updateById(order));
        calcFee(order.getUuid());
        return checks;
    }

    private void markRollsRecorded(List<OriginalRoll> rolls, String operator, LocalDateTime now) {
        for (OriginalRoll roll : rolls) {
            roll.setIsChecked(1);
            roll.setCheckUser(operator);
            roll.setCheckTime(now);
            roll.setRollStatus(Integer.valueOf(PROCESS_MODE_DIRECT_SHIP).equals(roll.getProcessMode()) ? 4 : 3);
            ConcurrencyGuard.requireRowUpdated(originalRollMapper.updateById(roll));
        }
    }

    private void requireAllRollsRecorded(List<OriginalRoll> rolls) {
        List<String> missing = rolls.stream()
                .filter(roll -> !Integer.valueOf(1).equals(roll.getIsChecked()))
                .map(roll -> StringUtils.hasText(roll.getRollNo()) ? roll.getRollNo() : roll.getUuid())
                .toList();
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.E003,
                    "仍有母卷未回录，不能完成整单：" + String.join("、", missing));
        }
    }

    private int remainingRollCount(List<OriginalRoll> rolls) {
        return (int) rolls.stream().filter(roll -> !Integer.valueOf(1).equals(roll.getIsChecked())).count();
    }

    private void assignBackRecordWarehouse(List<FinishRoll> finishes,
                                            String warehouseUuid) {
        if (finishes.isEmpty()) {
            return;
        }
        finishes.forEach(finish -> finish.setWarehouseUuid(warehouseUuid));
        int updated = finishRollMapper.update(null, new LambdaUpdateWrapper<FinishRoll>()
                .in(FinishRoll::getUuid, finishes.stream().map(FinishRoll::getUuid).toList())
                .set(FinishRoll::getWarehouseUuid, warehouseUuid));
        if (updated != finishes.size()) {
            throw new BusinessException(ErrorCode.E006, "成品入库仓库更新不完整，请刷新后重试");
        }
    }

    private void markFirstStockInTime(List<FinishRoll> finishRolls, LocalDateTime stockInTime) {
        if (finishRolls.isEmpty()) {
            return;
        }
        finishRolls.stream()
                .filter(finish -> FINISH_STATUS_IN_STOCK == (finish.getFinishStatus() == null
                        ? 0 : finish.getFinishStatus()))
                .filter(finish -> finish.getStockInTime() == null)
                .forEach(finish -> finish.setStockInTime(stockInTime));
        finishRollMapper.update(null, new LambdaUpdateWrapper<FinishRoll>()
                .in(FinishRoll::getUuid, finishRolls.stream().map(FinishRoll::getUuid).toList())
                .eq(FinishRoll::getFinishStatus, FINISH_STATUS_IN_STOCK)
                .isNull(FinishRoll::getStockInTime)
                .set(FinishRoll::getStockInTime, stockInTime));
    }

    private void authorizeBlockRelease(BackRecordDTO dto, ProcessOrder order,
                                       BackRecordResultVO.RollCheck blockCheck) {
        if (!StringUtils.hasText(dto.getReleaseAdminUsername())
                || !StringUtils.hasText(dto.getReleaseAdminPassword())
                || !StringUtils.hasText(dto.getReleaseReason())) {
            throw new BusinessException(ErrorCode.E005,
                    "重量偏差超过5%，需管理员账号密码授权并填写原因");
        }
        AdminCredentialVerifier.VerifiedAdmin admin = adminCredentialVerifier.verify(
                dto.getReleaseAdminUsername(), dto.getReleaseAdminPassword());
        String detail = "卷号=" + blockCheck.getRollNo()
                + "，偏差率=" + blockCheck.getDiffRatioPct() + "%"
                + "，授权账号=" + admin.username()
                + "，提交人=" + currentOperator()
                + "，原因：" + dto.getReleaseReason();
        operationLogService.recordVerifiedActor(BIZ_TYPE_ORDER, order.getUuid(), order.getOrderNo(),
                OperationLogService.ACTION_OVER_TOLERANCE_RELEASE, admin.displayName(), detail);
    }

    private void confirmWarnVariance(BackRecordDTO dto, ProcessOrder order,
                                     BackRecordResultVO.RollCheck warnCheck) {
        if (warnCheck == null) {
            return;
        }
        if (!StringUtils.hasText(dto.getVarianceReason())) {
            throw new BusinessException(ErrorCode.E007, "重量偏差处于警告范围，请填写原因后继续");
        }
        String detail = "卷号=" + warnCheck.getRollNo()
                + "，偏差率=" + warnCheck.getDiffRatioPct() + "%"
                + "，原因：" + dto.getVarianceReason();
        operationLogService.record(BIZ_TYPE_ORDER, order.getUuid(), order.getOrderNo(),
                OperationLogService.ACTION_WEIGHT_VARIANCE_CONFIRM, currentOperator(), detail);
    }

    private List<BackRecordFinishDTO> standardFinishDtos(
            List<BackRecordFinishDTO> dtos, Set<String> onSiteUuids) {
        return (dtos == null ? List.<BackRecordFinishDTO>of() : dtos).stream()
                .filter(dto -> StringUtils.hasText(dto.getUuid()) && !onSiteUuids.contains(dto.getUuid()))
                .toList();
    }

    private List<FinishRoll> standardFinishRolls(List<FinishRoll> finishes, Set<String> onSiteUuids) {
        return finishes.stream()
                .filter(finish -> !onSiteUuids.contains(finish.getUuid()))
                .toList();
    }

    /** 写入原纸复称实际克重/门幅/重量。actualWeight 是闭合与计费基准，必填非负。 */
    private void writeRollActuals(List<BackRecordRollDTO> rollDtos, Map<String, OriginalRoll> rollByUuid) {
        if (rollDtos == null) {
            return;
        }
        for (BackRecordRollDTO d : rollDtos) {
            OriginalRoll roll = rollByUuid.get(d.getUuid());
            if (roll == null) {
                throw new BusinessException("原纸单卷不存在：" + d.getUuid());
            }
            if (d.getActualWeight() == null || d.getActualWeight().signum() <= 0) {
                throw new BusinessException("原纸复称实际重量必须大于0：" + roll.getRollNo());
            }
            roll.setActualGramWeight(d.getActualGramWeight());
            roll.setActualWidth(d.getActualWidth());
            roll.setActualWeight(d.getActualWeight());
            if (StringUtils.hasText(d.getRemark())) {
                roll.setRemark(d.getRemark());
            }
            ConcurrencyGuard.requireRowUpdated(originalRollMapper.updateById(roll));
        }
    }

    /** 写入各工序损耗。未填写按0处理，避免闭合公式继续使用旧值或空值。 */
    private void writeStepLosses(List<BackRecordStepDTO> stepDtos, List<ProcessStep> steps) {
        if (stepDtos == null) {
            return;
        }
        Map<String, ProcessStep> stepByUuid = new LinkedHashMap<>();
        for (ProcessStep step : steps) {
            stepByUuid.put(step.getUuid(), step);
        }
        for (BackRecordStepDTO dto : stepDtos) {
            ProcessStep step = stepByUuid.get(dto.getUuid());
            if (step == null) {
                throw new BusinessException("工序记录不存在：" + dto.getUuid());
            }
            BigDecimal lossWeight = normalizeLossWeight(dto.getLossWeight());
            step.setLossWeight(lossWeight);
            if (dto.getKnifeCount() != null) {
                step.setKnifeCount(dto.getKnifeCount());
            }
            ConcurrencyGuard.requireRowUpdated(processStepMapper.updateById(step));
        }
    }

    private void validateOnSiteActualSteps(List<OriginalRoll> rolls, List<ProcessStep> steps,
                                           List<BackRecordStepDTO> stepDtos) {
        Set<String> onSiteSawRolls = rolls.stream()
                .filter(roll -> isOnSite(roll)
                        && Integer.valueOf(FeeCalculator.STEP_TYPE_SAW).equals(roll.getMainStepType()))
                .map(OriginalRoll::getUuid)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> requiredStepUuids = steps.stream()
                .filter(step -> onSiteSawRolls.contains(step.getOriginalUuid())
                        && Integer.valueOf(STEP_MAIN).equals(step.getIsMain()))
                .map(ProcessStep::getUuid)
                .collect(java.util.stream.Collectors.toSet());
        Map<String, BackRecordStepDTO> submitted = (stepDtos == null
                ? List.<BackRecordStepDTO>of() : stepDtos).stream()
                .collect(java.util.stream.Collectors.toMap(BackRecordStepDTO::getUuid, dto -> dto));
        boolean missingKnifeCount = requiredStepUuids.stream().anyMatch(uuid -> {
            BackRecordStepDTO dto = submitted.get(uuid);
            return dto == null || dto.getKnifeCount() == null || dto.getKnifeCount() <= 0;
        });
        if (missingKnifeCount) {
            throw new BusinessException("现场定尺锯纸必须填写实际刀数");
        }
    }

    /** 按原纸汇总工序、报废、修边损耗，并回写损耗率，供报表与出库快照读取。 */
    private void updateRollLossSummaries(List<OriginalRoll> rolls, List<ProcessStep> steps,
                                         List<FinishRoll> finishRolls, List<FinishOriginalRel> rels) {
        Map<String, BigDecimal> lossByRoll = RollLossSummaryCalculator.calculate(rolls, steps, finishRolls, rels);
        for (OriginalRoll roll : rolls) {
            BigDecimal lossWeight = lossByRoll.getOrDefault(roll.getUuid(), BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP);
            roll.setTotalLossWeight(lossWeight);
            roll.setTotalLossRatio(lossRatio(lossWeight, rollLossBaseWeight(roll)));
            ConcurrencyGuard.requireRowUpdated(originalRollMapper.updateById(roll));
        }
    }

    private BigDecimal normalizeLossWeight(BigDecimal lossWeight) {
        if (lossWeight == null) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
        if (lossWeight.signum() < 0) {
            throw new BusinessException("工序损耗不能为负数");
        }
        return lossWeight.setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal lossRatio(BigDecimal lossWeight, BigDecimal baseWeight) {
        if (baseWeight == null || baseWeight.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return lossWeight.divide(baseWeight, 6, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal rollLossBaseWeight(OriginalRoll roll) {
        if (roll.getActualWeight() != null && roll.getActualWeight().signum() > 0) {
            return roll.getActualWeight();
        }
        BigDecimal rollWeight = roll.getRollWeight() == null ? BigDecimal.ZERO : roll.getRollWeight();
        BigDecimal pieces = BigDecimal.valueOf(roll.getPieceNum() == null ? 1 : roll.getPieceNum());
        return rollWeight.multiply(pieces);
    }

    /**
     * 直发原纸(process_mode=3)三不约束：回录时自动产出 source_type=2 直发成品，
     * 沿用母卷号、跳过待入库直接已入库(2)，不拆分、不进入加工流程。
     * 已存在该原纸的直发成品则跳过（幂等，防重复回录产生多条）。
     */
    private int generateDirectShipFinishes(ProcessOrder order, List<OriginalRoll> rolls,
                                           List<FinishRoll> existingFinishes) {
        int generated = 0;
        int rowSort = existingFinishes.isEmpty() ? 0
                : existingFinishes.stream().mapToInt(f -> f.getRowSort() == null ? 0 : f.getRowSort()).max().orElse(0);
        for (OriginalRoll roll : rolls) {
            if (roll.getProcessMode() == null || roll.getProcessMode() != PROCESS_MODE_DIRECT_SHIP) {
                continue;
            }
            boolean already = existingFinishes.stream()
                    .anyMatch(f -> SOURCE_DIRECT_SHIP == (f.getSourceType() == null ? 0 : f.getSourceType())
                            && ROLL_NO_VOID != (f.getRollNoStatus() == null ? 0 : f.getRollNoStatus())
                            && roll.getRollNo() != null && roll.getRollNo().equals(f.getFinishRollNo()));
            if (already) {
                continue;
            }
            FinishRoll f = new FinishRoll();
            f.setOrderUuid(order.getUuid());
            f.setRowSort(++rowSort);
            f.setFinishRollNo(roll.getRollNo()); // 沿用母卷号，不占字母流水。
            f.setRollNoStatus(ROLL_NO_PRE);
            f.setIsSpare(IS_SPARE_NO);
            f.setPaperName(roll.getPaperName());
            f.setGramWeight(roll.getActualGramWeight() != null ? roll.getActualGramWeight() : roll.getGramWeight());
            f.setFinishWidth(roll.getActualWidth() != null ? roll.getActualWidth() : roll.getOriginalWidth());
            f.setSourceType(SOURCE_DIRECT_SHIP);
            f.setActualWeight(roll.getActualWeight()); // 直发：成品重量=母卷复称重量。
            f.setRemainingWeight(roll.getActualWeight());
            f.setEstimateWeight(roll.getActualWeight());
            f.setFinishStatus(FINISH_STATUS_IN_STOCK); // 跳过待入库直接已入库。
            f.setWarehouseUuid(order.getWarehouseUuid());
            f.setOriginalRollNos(roll.getRollNo());
            finishRollMapper.insert(f);
            existingFinishes.add(f);
            generated++;
        }
        return generated;
    }

    /** 加工产出成品(source_type=1 且未作废卷号)置已入库(2)；未使用的备用号作废封存(3)。 */
    private int finishProcessRollsAndVoidSpare(List<FinishRoll> finishRolls) {
        int voided = 0;
        for (FinishRoll f : finishRolls) {
            boolean directShip = SOURCE_DIRECT_SHIP == (f.getSourceType() == null ? 0 : f.getSourceType());
            if (directShip) {
                continue; // 直发已在生成时入库。
            }
            boolean isSpare = IS_SPARE_YES == (f.getIsSpare() == null ? 0 : f.getIsSpare());
            boolean unused = f.getActualWeight() == null;
            if (isSpare && unused && (f.getRollNoStatus() == null || f.getRollNoStatus() != ROLL_NO_VOID)) {
                // 未实际使用的备用号 → 作废封存，占用索引不再分配。
                f.setRollNoStatus(ROLL_NO_VOID);
                ConcurrencyGuard.requireRowUpdated(finishRollMapper.updateById(f));
                voided++;
            } else if (!unused) {
                f.setRollNoStatus(2);
                f.setFinishStatus(FINISH_STATUS_IN_STOCK);
                ConcurrencyGuard.requireRowUpdated(finishRollMapper.updateById(f));
            }
        }
        return voided;
    }

    /**
     * 整单闭合校验：基准=Σ原纸复称实际重量；
     * 理论合计=Σ成品实际重量 + Σ工序损耗 + Σ成品报废 + Σ成品修边。
     */
    private WeightCheckCalculator.CheckResult computeOrderClosure(List<OriginalRoll> rolls,
                                                                  List<FinishRoll> finishRolls,
                                                                  List<ProcessStep> steps) {
        BigDecimal wActual = BigDecimal.ZERO;
        for (OriginalRoll r : rolls) {
            if (r.getActualWeight() != null) {
                wActual = wActual.add(r.getActualWeight());
            }
        }
        BigDecimal finishSum = BigDecimal.ZERO;
        BigDecimal scrapSum = BigDecimal.ZERO;
        BigDecimal trimSum = BigDecimal.ZERO;
        for (FinishRoll f : finishRolls) {
            if (SOURCE_DIRECT_SHIP == (f.getSourceType() == null ? 0 : f.getSourceType())) {
                // 直发成品重量=母卷重量，不参与加工闭合（否则与基准重复计入）。
                continue;
            }
            if (f.getActualWeight() != null) {
                finishSum = finishSum.add(f.getActualWeight());
            }
            if (f.getScrapWeight() != null) {
                scrapSum = scrapSum.add(f.getScrapWeight());
            }
            if (f.getTrimWeightShare() != null) {
                trimSum = trimSum.add(f.getTrimWeightShare());
            }
        }
        BigDecimal lossSum = BigDecimal.ZERO;
        for (ProcessStep s : steps) {
            if (s.getLossWeight() != null) {
                lossSum = lossSum.add(s.getLossWeight());
            }
        }
        // 直发母卷重量从基准中剔除：基准只对加工卷闭合。
        BigDecimal directBase = BigDecimal.ZERO;
        for (OriginalRoll r : rolls) {
            if (r.getProcessMode() != null && r.getProcessMode() == PROCESS_MODE_DIRECT_SHIP
                    && r.getActualWeight() != null) {
                directBase = directBase.add(r.getActualWeight());
            }
        }
        wActual = wActual.subtract(directBase);
        return WeightCheckCalculator.check(wActual, finishSum, lossSum, scrapSum, trimSum,
                weightCheckThresholdService.currentThresholds());
    }

    private List<BackRecordResultVO.RollCheck> computeClosureChecks(ProcessOrder order, List<OriginalRoll> rolls,
                                                                    List<FinishRoll> finishRolls,
                                                                    List<ProcessStep> steps,
                                                                    List<FinishOriginalRel> rels) {
        if (rels == null || rels.isEmpty()) {
            return List.of(toRollCheck(null, order, computeOrderClosure(rolls, finishRolls, steps)));
        }

        Map<String, OriginalRoll> rollByUuid = new LinkedHashMap<>();
        for (OriginalRoll roll : rolls) {
            rollByUuid.put(roll.getUuid(), roll);
        }
        Map<String, FinishRoll> finishByUuid = new LinkedHashMap<>();
        for (FinishRoll finish : finishRolls) {
            finishByUuid.put(finish.getUuid(), finish);
        }
        Map<String, ClosureBucket> buckets = new LinkedHashMap<>();
        for (OriginalRoll roll : rolls) {
            if (roll.getProcessMode() == null || roll.getProcessMode() != PROCESS_MODE_DIRECT_SHIP) {
                buckets.put(roll.getUuid(), new ClosureBucket(roll));
            }
        }
        for (ProcessStep step : steps) {
            ClosureBucket bucket = buckets.get(step.getOriginalUuid());
            if (bucket != null && step.getLossWeight() != null) {
                bucket.lossSum = bucket.lossSum.add(step.getLossWeight());
            }
        }
        for (FinishOriginalRel rel : rels) {
            ClosureBucket bucket = buckets.get(rel.getOriginalUuid());
            FinishRoll finish = finishByUuid.get(rel.getFinishUuid());
            if (bucket == null || finish == null
                    || SOURCE_DIRECT_SHIP == (finish.getSourceType() == null ? 0 : finish.getSourceType())
                    || ROLL_NO_VOID == (finish.getRollNoStatus() == null ? 0 : finish.getRollNoStatus())
                    || IS_SPARE_YES == (finish.getIsSpare() == null ? 0 : finish.getIsSpare())) {
                continue;
            }
            BigDecimal ratio = rel.getShareRatio() == null ? HUNDRED : rel.getShareRatio();
            bucket.finishSum = bucket.finishSum.add(weightShare(finish.getActualWeight(), ratio));
            bucket.scrapSum = bucket.scrapSum.add(weightShare(finish.getScrapWeight(), ratio));
            bucket.trimSum = bucket.trimSum.add(weightShare(finish.getTrimWeightShare(), ratio));
        }

        WeightCheckCalculator.Thresholds thresholds = weightCheckThresholdService.currentThresholds();
        List<BackRecordResultVO.RollCheck> checks = new ArrayList<>(buckets.size());
        for (ClosureBucket bucket : buckets.values()) {
            WeightCheckCalculator.CheckResult check = WeightCheckCalculator.check(
                    bucket.roll.getActualWeight(), bucket.finishSum, bucket.lossSum, bucket.scrapSum,
                    bucket.trimSum, thresholds);
            checks.add(toRollCheck(bucket.roll, null, check));
        }
        return checks;
    }

    private BigDecimal weightShare(BigDecimal weight, BigDecimal shareRatio) {
        if (weight == null) {
            return BigDecimal.ZERO;
        }
        return weight.multiply(shareRatio).divide(HUNDRED, 3, RoundingMode.HALF_UP);
    }

    private BackRecordResultVO.RollCheck summaryClosure(List<BackRecordResultVO.RollCheck> checks) {
        BackRecordResultVO.RollCheck summary = new BackRecordResultVO.RollCheck();
        BigDecimal actual = BigDecimal.ZERO;
        BigDecimal theoretical = BigDecimal.ZERO;
        boolean hasBlock = false;
        boolean hasWarn = false;
        for (BackRecordResultVO.RollCheck check : checks) {
            actual = actual.add(check.getActualWeight() == null ? BigDecimal.ZERO : check.getActualWeight());
            theoretical = theoretical.add(check.getTheoreticalWeight() == null ? BigDecimal.ZERO : check.getTheoreticalWeight());
            hasBlock = hasBlock || WeightCheckCalculator.Level.BLOCK.name().equals(check.getLevel());
            hasWarn = hasWarn || WeightCheckCalculator.Level.WARN.name().equals(check.getLevel());
        }
        BigDecimal diff = actual.subtract(theoretical);
        BigDecimal ratio = actual.signum() == 0 ? BigDecimal.ZERO
                : diff.abs().divide(actual, 6, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        summary.setLevel(hasBlock ? WeightCheckCalculator.Level.BLOCK.name()
                : hasWarn ? WeightCheckCalculator.Level.WARN.name() : WeightCheckCalculator.Level.PASS.name());
        summary.setActualWeight(actual.setScale(3, RoundingMode.HALF_UP));
        summary.setTheoreticalWeight(theoretical.setScale(3, RoundingMode.HALF_UP));
        summary.setDiffWeight(diff.setScale(3, RoundingMode.HALF_UP));
        summary.setDiffRatioPct(ratio.setScale(2, RoundingMode.HALF_UP));
        return summary;
    }

    private BackRecordResultVO.RollCheck firstBlockCheck(List<BackRecordResultVO.RollCheck> checks) {
        for (BackRecordResultVO.RollCheck check : checks) {
            if (WeightCheckCalculator.Level.BLOCK.name().equals(check.getLevel())) {
                return check;
            }
        }
        return null;
    }

    private BackRecordResultVO.RollCheck firstWarnCheck(List<BackRecordResultVO.RollCheck> checks) {
        for (BackRecordResultVO.RollCheck check : checks) {
            if (WeightCheckCalculator.Level.WARN.name().equals(check.getLevel())) {
                return check;
            }
        }
        return null;
    }

    private BackRecordResultVO.RollCheck toRollCheck(OriginalRoll roll, ProcessOrder order,
                                                     WeightCheckCalculator.CheckResult check) {
        BackRecordResultVO.RollCheck rc = new BackRecordResultVO.RollCheck();
        rc.setOriginalUuid(roll == null ? order.getUuid() : roll.getUuid());
        rc.setRollNo(roll == null ? order.getOrderNo() : roll.getRollNo());
        rc.setLevel(check.level.name());
        rc.setActualWeight(check.actualWeight);
        rc.setTheoreticalWeight(check.theoreticalWeight);
        rc.setDiffWeight(check.diffWeight);
        rc.setDiffRatioPct(check.diffRatioPct);
        return rc;
    }

    private static final class ClosureBucket {
        private final OriginalRoll roll;
        private BigDecimal finishSum = BigDecimal.ZERO;
        private BigDecimal lossSum = BigDecimal.ZERO;
        private BigDecimal scrapSum = BigDecimal.ZERO;
        private BigDecimal trimSum = BigDecimal.ZERO;

        private ClosureBucket(OriginalRoll roll) {
            this.roll = roll;
        }
    }

    /** 完成快照 snap_finish：锁定回录后的原纸实际参数与成品实际重量、闭合结论。 */
    private String buildSnapFinish(ProcessOrder order, List<OriginalRoll> rolls,
                                   List<FinishRoll> finishRolls, LocalDateTime recordTime,
                                   List<BackRecordResultVO.RollCheck> rollChecks,
                                   BackRecordWarehousePolicy.WarehouseSnapshot warehouse) {
        List<FinishRoll> printableFinishes = ProcessOrderPrintFinishPolicy.printable(finishRolls);
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("schema_version", SNAP_SCHEMA_VERSION);
        snap.put("order_no", order.getOrderNo());
        snap.put("customer_name", order.getCustomerName());
        snap.put("back_record_time", recordTime.toString());
        snap.put("back_record_user", order.getBackRecordUser());
        Map<String, Object> warehouseSnap = new LinkedHashMap<>();
        warehouseSnap.put("uuid", warehouse.uuid());
        warehouseSnap.put("name", warehouse.name());
        warehouseSnap.put("location", warehouse.location());
        snap.put("warehouse", warehouseSnap);

        BackRecordResultVO.RollCheck summaryCheck = summaryClosure(rollChecks);
        Map<String, Object> closure = new LinkedHashMap<>();
        closure.put("level", summaryCheck.getLevel());
        closure.put("actual_weight", summaryCheck.getActualWeight());
        closure.put("theoretical_weight", summaryCheck.getTheoreticalWeight());
        closure.put("diff_weight", summaryCheck.getDiffWeight());
        closure.put("diff_ratio_pct", summaryCheck.getDiffRatioPct());
        snap.put("closure", closure);

        List<Map<String, Object>> closureSnaps = new ArrayList<>(rollChecks.size());
        for (BackRecordResultVO.RollCheck check : rollChecks) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("original_uuid", check.getOriginalUuid());
            m.put("roll_no", check.getRollNo());
            m.put("level", check.getLevel());
            m.put("actual_weight", check.getActualWeight());
            m.put("theoretical_weight", check.getTheoreticalWeight());
            m.put("diff_weight", check.getDiffWeight());
            m.put("diff_ratio_pct", check.getDiffRatioPct());
            closureSnaps.add(m);
        }
        snap.put("roll_closures", closureSnaps);

        List<Map<String, Object>> rollSnaps = new ArrayList<>(rolls.size());
        for (OriginalRoll r : rolls) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("uuid", r.getUuid());
            m.put("roll_no", r.getRollNo());
            m.put("process_mode", r.getProcessMode());
            m.put("actual_gram_weight", r.getActualGramWeight());
            m.put("actual_width", r.getActualWidth());
            m.put("actual_weight", r.getActualWeight());
            rollSnaps.add(m);
        }
        snap.put("original_rolls", rollSnaps);

        List<Map<String, Object>> finishSnaps = new ArrayList<>(printableFinishes.size());
        for (FinishRoll f : printableFinishes) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("uuid", f.getUuid());
            m.put("finish_roll_no", f.getFinishRollNo());
            m.put("source_type", f.getSourceType());
            m.put("roll_no_status", f.getRollNoStatus());
            m.put("finish_status", f.getFinishStatus());
            m.put("warehouse_uuid", f.getWarehouseUuid());
            m.put("is_remain", f.getIsRemain());
            m.put("paper_name", f.getPaperName());
            m.put("gram_weight", f.getGramWeight());
            m.put("finish_width", f.getFinishWidth());
            m.put("finish_diameter", f.getFinishDiameter());
            m.put("finish_core_diameter", f.getFinishCoreDiameter());
            m.put("actual_weight", f.getActualWeight());
            m.put("scrap_weight", f.getScrapWeight());
            m.put("trim_weight_share", f.getTrimWeightShare());
            finishSnaps.add(m);
        }
        snap.put("finish_rolls", finishSnaps);
        ProcessOrderSnapshotDetailCodec.append(
                snap, ProcessOrderPrintDetailPolicy.filter(snapshotDetail(order)), objectMapper);

        String json;
        try {
            json = objectMapper.writeValueAsString(snap);
        } catch (JsonProcessingException e) {
            throw new BusinessException("完成快照序列化失败");
        }
        if (!json.contains("\"schema_version\"")) {
            throw new BusinessException("快照缺少 schema_version，拒绝写入");
        }
        return json;
    }

    private ProcessOrderDetailVO snapshotDetail(ProcessOrder order) {
        ProcessOrderDetailVO detail = getDetail(order.getUuid());
        detail.setOrder(order);
        return detail;
    }

    @Override
    public SnapshotDiffVO snapshotDiff(String uuid) {
        ProcessOrder order = getById(uuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        if (!StringUtils.hasText(order.getSnapPrint())) {
            throw new BusinessException(ErrorCode.E002, "下发快照尚未生成");
        }
        if (!StringUtils.hasText(order.getSnapFinish())) {
            throw new BusinessException(ErrorCode.E002, "完成快照尚未生成");
        }

        Map<String, Object> printSnap = readSnap(order.getSnapPrint());
        Map<String, Object> finishSnap = readSnap(order.getSnapFinish());

        // 按 uuid 配对：print 侧为标称值，finish 侧为实际值。
        Map<String, Map<String, Object>> printRolls = indexByUuid(printSnap.get("original_rolls"));
        Map<String, Map<String, Object>> finishRolls = indexByUuid(finishSnap.get("original_rolls"));
        Map<String, Map<String, Object>> printFinishes = indexByUuid(printSnap.get("finish_rolls"));
        Map<String, Map<String, Object>> finishFinishes = indexByUuid(finishSnap.get("finish_rolls"));

        SnapshotDiffVO vo = new SnapshotDiffVO();
        vo.setOrderUuid(order.getUuid());
        vo.setOrderNo(order.getOrderNo());

        List<SnapshotDiffVO.RollDiff> rollDiffs = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> e : printRolls.entrySet()) {
            Map<String, Object> p = e.getValue();
            Map<String, Object> f = finishRolls.get(e.getKey());
            SnapshotDiffVO.RollDiff d = new SnapshotDiffVO.RollDiff();
            d.setUuid(e.getKey());
            d.setRollNo(strVal(p.get("roll_no")));
            Integer printGram = toInt(p.get("gram_weight"));
            Integer finishGram = f == null ? null : toInt(f.get("actual_gram_weight"));
            d.setPrintGramWeight(printGram);
            d.setFinishGramWeight(finishGram);
            d.setGramWeightChanged(!java.util.Objects.equals(printGram, finishGram));
            Integer printWidth = toInt(p.get("original_width"));
            Integer finishWidth = f == null ? null : toInt(f.get("actual_width"));
            d.setPrintWidth(printWidth);
            d.setFinishWidth(finishWidth);
            d.setWidthChanged(!java.util.Objects.equals(printWidth, finishWidth));
            rollDiffs.add(d);
        }
        vo.setRollDiffs(rollDiffs);

        List<SnapshotDiffVO.FinishDiff> finishDiffs = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> e : printFinishes.entrySet()) {
            Map<String, Object> p = e.getValue();
            Map<String, Object> f = finishFinishes.get(e.getKey());
            SnapshotDiffVO.FinishDiff d = new SnapshotDiffVO.FinishDiff();
            d.setUuid(e.getKey());
            d.setFinishRollNo(strVal(p.get("finish_roll_no")));
            Integer printWidth = toInt(p.get("finish_width"));
            Integer actualWidth = f == null ? null : toInt(f.get("finish_width"));
            d.setPrintWidth(printWidth);
            d.setFinishWidth(actualWidth);
            d.setWidthChanged(!Objects.equals(printWidth, actualWidth));
            Integer printDiameter = toInt(p.get("finish_diameter"));
            Integer actualDiameter = f == null ? null : toInt(f.get("finish_diameter"));
            d.setPrintDiameter(printDiameter);
            d.setFinishDiameter(actualDiameter);
            d.setDiameterChanged(!Objects.equals(printDiameter, actualDiameter));
            BigDecimal est = toBigDecimal(p.get("estimate_weight"));
            BigDecimal act = f == null ? null : toBigDecimal(f.get("actual_weight"));
            d.setEstimateWeight(est);
            d.setActualWeight(act);
            d.setWeightChanged(!bdEquals(est, act));
            finishDiffs.add(d);
        }
        vo.setFinishDiffs(finishDiffs);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> uploadDamageImages(String rollUuid, MultipartFile[] files) {
        LockedRoll locked = lockRollAndOrder(rollUuid);
        validateRollProductionEditable(locked.order());
        return damageImageService.append(locked.roll(), files);
    }

    /** 详情读取保持兼容；写入时由 DamageImageService 严格校验，禁止覆盖损坏引用。 */
    private List<String> parseDamageImages(String json) {
        if (!StringUtils.hasText(json)) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException exception) {
            return new ArrayList<>();
        }
    }

    /** 反序列化快照 JSON 为 Map；失败抛业务异常，不暴露堆栈。 */
    private Map<String, Object> readSnap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new BusinessException("快照解析失败");
        }
    }

    /** 将快照中的明细列表以 uuid 为键建索引；非列表或缺 uuid 项跳过。 */
    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> indexByUuid(Object listObj) {
        Map<String, Map<String, Object>> index = new LinkedHashMap<>();
        if (!(listObj instanceof List<?> list)) {
            return index;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                Object id = m.get("uuid");
                if (id != null) {
                    index.put(String.valueOf(id), (Map<String, Object>) m);
                }
            }
        }
        return index;
    }

    private String strVal(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private Integer toInt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof BigDecimal bd) {
            return bd;
        }
        try {
            return new BigDecimal(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** BigDecimal 数值相等（忽略标度），任一为 null 时按 Objects.equals 处理。 */
    private boolean bdEquals(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }

    private BackRecordResultVO buildBackRecordResult(ProcessOrder order, LocalDateTime recordTime,
                                                     List<BackRecordResultVO.RollCheck> rollChecks,
                                                     int directShipGenerated, int voidedSpareCount,
                                                     int recordedRollCount, int remainingRollCount) {
        BackRecordResultVO vo = new BackRecordResultVO();
        vo.setOrderUuid(order.getUuid());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setBackRecordTime(recordTime);
        vo.setOrderCompleted(order.getOrderStatus() != null && order.getOrderStatus() == STATUS_FINISHED);
        vo.setRecordedRollCount(recordedRollCount);
        vo.setRemainingRollCount(remainingRollCount);
        vo.setOverToleranceReleased(firstBlockCheck(rollChecks) != null);
        vo.setDirectShipGenerated(directShipGenerated);
        vo.setVoidedSpareCount(voidedSpareCount);
        vo.setRollChecks(rollChecks);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FeeResultVO calcFee(String uuid) {
        businessLockService.lockProcessOrders(List.of(uuid));
        ProcessOrder order = getById(uuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        requireFeeCalculationStatus(order.getOrderStatus());

        Customer customer = customerService.getById(order.getCustomerUuid());
        BigDecimal sawPrice = customer == null ? null : customer.getSawPrice();
        BigDecimal rewindPrice = customer == null ? null : customer.getRewindPrice();

        List<OriginalRoll> rolls = originalRollMapper.selectList(
                new LambdaQueryWrapper<OriginalRoll>()
                        .eq(OriginalRoll::getOrderUuid, uuid)
                        .orderByAsc(OriginalRoll::getRowSort));
        List<ProcessStep> steps = processStepMapper.selectList(
                new LambdaQueryWrapper<ProcessStep>()
                        .eq(ProcessStep::getOrderUuid, uuid)
                        .orderByAsc(ProcessStep::getStepSort));
        Map<String, OriginalRoll> rollByUuid = new LinkedHashMap<>();
        for (OriginalRoll r : rolls) {
            rollByUuid.put(r.getUuid(), r);
        }

        List<FeeResultVO.StepFee> stepFees = new ArrayList<>(steps.size());
        Map<String, BigDecimal> processAmountByRoll = new LinkedHashMap<>();
        int actualTotalKnife = 0;
        boolean hasExtraStep = false;

        for (ProcessStep step : steps) {
            OriginalRoll roll = rollByUuid.get(step.getOriginalUuid());
            BigDecimal unitPrice = resolveUnitPrice(order, step, sawPrice, rewindPrice);
            BigDecimal effectiveUnitPrice = ProcessStepPricingBatchCalculator.effectivePrice(
                    unitPrice, step.getBillingUnitPrice());
            BigDecimal tonnage = resolveTonnage(step, roll);
            BigDecimal feeQuantity = resolveFeeQuantity(step, roll, tonnage);
            BigDecimal standardQuantity = standardQuantity(step, feeQuantity);
            BigDecimal standardAmount = FeeCalculator.stepAmount(
                    step.getStepType(), step.getKnifeCount(), feeQuantity, unitPrice);
            ProcessStepPricingPolicy.Result pricing = ProcessStepPricingPolicy.calculate(
                    step, standardQuantity, standardAmount, effectiveUnitPrice);
            BigDecimal amount = pricing.finalAmount();

            step.setUnitPrice(unitPrice);
            if (isServiceStep(step.getStepType())) {
                step.setServiceQuantity(feeQuantity);
            }
            if (step.getStepType() != null && step.getStepType() == FeeCalculator.STEP_TYPE_REWIND) {
                step.setProcessWeight(tonnage);
            }
            step.setBillingMode(pricing.mode());
            step.setStandardQuantity(pricing.standardQuantity());
            step.setBillingQuantity(pricing.billingQuantity());
            step.setStandardStepAmount(pricing.standardAmount());
            step.setPricingAdjustmentAmount(pricing.adjustmentAmount());
            step.setStepAmount(amount);
            ConcurrencyGuard.requireRowUpdated(processStepMapper.updateById(step));

            if (step.getStepType() != null && step.getStepType() == FeeCalculator.STEP_TYPE_SAW
                    && step.getKnifeCount() != null) {
                actualTotalKnife += step.getKnifeCount();
            }
            if (step.getIsMain() != null && step.getIsMain() != STEP_MAIN) {
                hasExtraStep = true;
            }
            processAmountByRoll.merge(step.getOriginalUuid(), amount, BigDecimal::add);

            FeeResultVO.StepFee sf = new FeeResultVO.StepFee();
            sf.setStepUuid(step.getUuid());
            sf.setOriginalUuid(step.getOriginalUuid());
            sf.setStepType(step.getStepType());
            sf.setUnitPrice(unitPrice);
            sf.setBillingUnitPrice(step.getBillingUnitPrice());
            sf.setEffectiveUnitPrice(effectiveUnitPrice);
            sf.setQuantity(pricing.billingQuantity());
            sf.setStandardQuantity(pricing.standardQuantity());
            sf.setStandardStepAmount(pricing.standardAmount());
            sf.setBillingMode(pricing.mode());
            sf.setPricingAdjustmentAmount(pricing.adjustmentAmount());
            sf.setStepAmount(amount);
            stepFees.add(sf);
        }

        // 单卷加工费回写。
        List<FeeResultVO.RollFee> rollFees = new ArrayList<>(rolls.size());
        BigDecimal totalProcessAmount = BigDecimal.ZERO;
        BigDecimal totalOriginalWeight = BigDecimal.ZERO;
        for (OriginalRoll roll : rolls) {
            BigDecimal rollAmount = processAmountByRoll.getOrDefault(roll.getUuid(), BigDecimal.ZERO);
            roll.setProcessAmount(rollAmount);
            ConcurrencyGuard.requireRowUpdated(originalRollMapper.updateById(roll));
            totalProcessAmount = totalProcessAmount.add(rollAmount);
            if (roll.getActualWeight() != null) {
                totalOriginalWeight = totalOriginalWeight.add(roll.getActualWeight());
            }

            FeeResultVO.RollFee rf = new FeeResultVO.RollFee();
            rf.setOriginalUuid(roll.getUuid());
            rf.setRollNo(roll.getRollNo());
            rf.setProcessAmount(rollAmount);
            rollFees.add(rf);
        }

        BigDecimal totalExtraAmount = sumExtraFees(order);
        boolean invoice = order.getIsInvoice() != null && order.getIsInvoice() == 1;
        FeeCalculator.OrderFee fee = FeeCalculator.assemble(
                totalProcessAmount, totalExtraAmount, invoice, order.getTaxRate());

        BigDecimal totalFinishWeight = sumFinishActualWeight(uuid);

        order.setProcessAmountNoTax(fee.processAmountNoTax);
        order.setProcessAmountTax(fee.processAmountTax);
        order.setExtraAmountNoTax(fee.extraAmountNoTax);
        order.setExtraAmountTax(fee.extraAmountTax);
        order.setTotalAmountNoTax(fee.totalAmountNoTax);
        order.setTotalAmountTax(fee.totalAmountTax);
        order.setTotalProcessAmount(totalProcessAmount.setScale(2, RoundingMode.HALF_UP));
        order.setTotalExtraAmount(totalExtraAmount.setScale(2, RoundingMode.HALF_UP));
        order.setTotalAmount(fee.totalAmount);
        order.setActualTotalKnife(actualTotalKnife);
        order.setTotalStepCount(steps.size());
        order.setHasExtraStep(hasExtraStep ? 1 : 0);
        order.setIsMixProcess(ProcessMixProcessResolver.isMix(steps) ? 1 : 0);
        order.setTotalOriginalWeight(totalOriginalWeight);
        order.setTotalOriginalTon(totalOriginalWeight.divide(FeeCalculator.TON_DIVISOR, 3, RoundingMode.HALF_UP));
        order.setTotalFinishWeight(totalFinishWeight);
        ConcurrencyGuard.requireUpdated(updateById(order));

        FeeResultVO vo = new FeeResultVO();
        vo.setOrderUuid(order.getUuid());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalProcessAmount(order.getTotalProcessAmount());
        vo.setTotalExtraAmount(order.getTotalExtraAmount());
        vo.setTotalAmountNoTax(fee.totalAmountNoTax);
        vo.setTotalAmountTax(fee.totalAmountTax);
        vo.setTotalAmount(fee.totalAmount);
        vo.setActualTotalKnife(actualTotalKnife);
        vo.setIsMixProcess(order.getIsMixProcess());
        vo.setRollFees(rollFees);
        vo.setStepFees(stepFees);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinishConfigBatchSaveVO saveFinishConfigBatch(String orderUuid, FinishConfigBatchSaveDTO dto) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = getById(orderUuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        validateFinishConfigBatchItems(orderUuid, dto);

        FinishConfigBatchSaveVO result = new FinishConfigBatchSaveVO();
        result.setOrderUuid(orderUuid);
        for (FinishConfigBatchSaveDTO.FinishConfigBatchItemDTO item : dto.getItems()) {
            result.getResults().add(saveFinishConfigInternal(orderUuid, item.getRollUuid(), item.getConfig()));
        }
        calcFee(orderUuid);
        return result;
    }

    private void validateFinishConfigBatchItems(String orderUuid, FinishConfigBatchSaveDTO dto) {
        Set<String> rollUuids = new LinkedHashSet<>();
        for (FinishConfigBatchSaveDTO.FinishConfigBatchItemDTO item : dto.getItems()) {
            if (!rollUuids.add(item.getRollUuid())) {
                throw new BusinessException("同一母卷不能重复提交成品配置");
            }
        }
        List<OriginalRoll> rolls = originalRollMapper.selectBatchIds(rollUuids);
        boolean allBelongToOrder = rolls.size() == rollUuids.size()
                && rolls.stream().allMatch(roll -> orderUuid.equals(roll.getOrderUuid()));
        if (!allBelongToOrder) {
            throw new BusinessException(ErrorCode.E002, "母卷明细不存在");
        }
    }

    static void requireFeeCalculationStatus(Integer status) {
        if (status == null || status < STATUS_DRAFT || status > STATUS_FINISHED) {
            throw new BusinessException("仅草稿/待下发/加工中/待回录/已完成状态可计费");
        }
    }

    /** 单价：step.unit_price 非空优先；为空按工艺回退客户档案 saw_price/rewind_price。 */
    private BigDecimal resolveUnitPrice(ProcessOrder order, ProcessStep step, BigDecimal sawPrice, BigDecimal rewindPrice) {
        if (step.getUnitPrice() != null && step.getUnitPrice().signum() > 0
                && !isLegacyDefaultPrice(order, step, sawPrice, rewindPrice)) {
            return step.getUnitPrice();
        }
        Integer type = step.getStepType();
        if (type != null && type == FeeCalculator.STEP_TYPE_SAW) {
            return sawPrice;
        }
        if (type != null && type == FeeCalculator.STEP_TYPE_REWIND) {
            return rewindPrice;
        }
        return null;
    }

    private boolean isLegacyDefaultPrice(ProcessOrder order, ProcessStep step,
                                         BigDecimal sawPrice, BigDecimal rewindPrice) {
        if (!isDraftOrPending(order)) {
            return false;
        }
        if (step.getStepType() != null && step.getStepType() == FeeCalculator.STEP_TYPE_SAW) {
            return shouldUseCustomerPrice(step.getUnitPrice(), LEGACY_DEFAULT_SAW_PRICE, sawPrice);
        }
        if (step.getStepType() != null && step.getStepType() == FeeCalculator.STEP_TYPE_REWIND) {
            return shouldUseCustomerPrice(step.getUnitPrice(), LEGACY_DEFAULT_REWIND_PRICE, rewindPrice);
        }
        return false;
    }

    private boolean isDraftOrPending(ProcessOrder order) {
        Integer status = order.getOrderStatus();
        return status == null || status == STATUS_DRAFT || status == STATUS_PENDING;
    }

    private boolean shouldUseCustomerPrice(BigDecimal stepPrice, BigDecimal legacyPrice, BigDecimal customerPrice) {
        return customerPrice != null
                && customerPrice.signum() >= 0
                && stepPrice != null
                && stepPrice.compareTo(legacyPrice) == 0
                && stepPrice.compareTo(customerPrice) != 0;
    }

    /** 复卷吨位：step.process_weight 非空优先；为空取原纸 actual_weight/1000 折吨。 */
    private BigDecimal resolveTonnage(ProcessStep step, OriginalRoll roll) {
        if (step.getStepType() == null || step.getStepType() != FeeCalculator.STEP_TYPE_REWIND) {
            return null;
        }
        if (step.getProcessWeight() != null && step.getProcessWeight().signum() > 0) {
            return step.getProcessWeight();
        }
        if (roll != null && roll.getActualWeight() != null) {
            return roll.getActualWeight().divide(FeeCalculator.TON_DIVISOR, 3, RoundingMode.HALF_UP);
        }
        return null;
    }

    private BigDecimal resolveFeeQuantity(ProcessStep step, OriginalRoll roll, BigDecimal tonnage) {
        if (step.getStepType() != null && step.getStepType() == FeeCalculator.STEP_TYPE_REWIND) {
            return tonnage;
        }
        return isServiceStep(step.getStepType())
                ? ServiceStepQuantityResolver.resolve(step.getBillingBasis(), roll) : null;
    }

    private BigDecimal standardQuantity(ProcessStep step, BigDecimal feeQuantity) {
        if (step.getStepType() != null && step.getStepType() == FeeCalculator.STEP_TYPE_SAW) {
            return step.getKnifeCount() == null ? null : BigDecimal.valueOf(step.getKnifeCount());
        }
        return feeQuantity;
    }

    /** 附加费合计 = 加急+托盘+装卸+运费+其他杂费。 */
    private BigDecimal sumExtraFees(ProcessOrder order) {
        return nz(order.getUrgentFee())
                .add(nz(order.getPalletFee()))
                .add(nz(order.getLoadingFee()))
                .add(nz(order.getFreightFee()))
                .add(nz(order.getOtherFee()));
    }

    private BigDecimal sumFinishActualWeight(String orderUuid) {
        List<FinishRoll> finishRolls = finishRollMapper.selectList(
                new LambdaQueryWrapper<FinishRoll>().eq(FinishRoll::getOrderUuid, orderUuid));
        BigDecimal sum = BigDecimal.ZERO;
        for (FinishRoll f : finishRolls) {
            if (isFormalFinishRoll(f) && f.getActualWeight() != null) {
                sum = sum.add(f.getActualWeight());
            }
        }
        return sum;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** DTO → OriginalRoll，回填快照、默认值、total_weight。 */
    private OriginalRoll buildRoll(OriginalRollDTO dto, ProcessOrder order, int rowSort) {
        OriginalRoll roll = new OriginalRoll();
        BeanUtils.copyProperties(dto, roll);
        roll.setOrderUuid(order.getUuid());
        roll.setRowSort(rowSort);
        roll.setCustomerName(order.getCustomerName());
        roll.setOrderNo(order.getOrderNo());
        roll.setRollStatus(ROLL_STATUS_PENDING);
        if (roll.getPieceNum() == null) {
            roll.setPieceNum(DEFAULT_PIECE_NUM);
        }
        if (roll.getProcessMode() == null) {
            roll.setProcessMode(DEFAULT_PROCESS_MODE);
        }
        validateMainStepType(roll);
        roll.setTotalWeight(calcTotalWeight(roll.getRollWeight(), roll.getPieceNum()));
        return roll;
    }

    private void validateMainStepType(OriginalRoll roll) {
        ProcessModePolicy.requireValid(roll.getProcessMode(), roll.getMainStepType());
    }

    private void validateCreateMainProcesses(List<OriginalRollDTO> rolls) {
        Set<Integer> mainStepTypes = new LinkedHashSet<>();
        for (OriginalRollDTO dto : rolls) {
            OriginalRoll roll = new OriginalRoll();
            roll.setProcessMode(dto.getProcessMode() == null ? DEFAULT_PROCESS_MODE : dto.getProcessMode());
            roll.setMainStepType(dto.getMainStepType());
            validateMainStepType(roll);
            if (isServiceOnly(roll)) {
                throw new BusinessException("仅附加工艺请使用加工单工作台创建");
            }
            if (requiresMainProcess(roll)) {
                mainStepTypes.add(roll.getMainStepType());
            }
        }
        processCatalogStepValidator.validateMainProcesses(mainStepTypes);
    }

    private void validateMainProcessCatalog(OriginalRoll roll) {
        if (requiresMainProcess(roll)) {
            processCatalogStepValidator.validateMainProcess(roll.getMainStepType());
        }
    }

    private void createMainStepIfNeeded(ProcessOrder order, OriginalRoll roll) {
        if (!requiresMainProcess(roll)) {
            return;
        }
        ProcessStep step = new ProcessStep();
        step.setOrderUuid(order.getUuid());
        step.setOriginalUuid(roll.getUuid());
        step.setStepSort(1);
        step.setStepType(roll.getMainStepType());
        step.setIsMain(STEP_MAIN);
        processStepMapper.insert(step);
    }

    private void validateRewindPreviewPlan(RewindPlanPreviewDTO dto) {
        Integer rewindMode = dto.getRewindMode();
        if (rewindMode == null || rewindMode < 1 || rewindMode > 6) {
            throw new BusinessException("复卷模式只能选择1到6类");
        }
        if (dto.getSegments() == null || dto.getSegments().isEmpty()) {
            throw new BusinessException("复卷方案至少需要一个直径分段");
        }
        for (RewindPlanPreviewDTO.RewindSegmentDTO segment : dto.getSegments()) {
            if (segment.getLayoutItems() == null || segment.getLayoutItems().isEmpty()) {
                throw new BusinessException("每个直径分段至少需要一个门幅排布");
            }
            if ((rewindMode == 2 || rewindMode == 3) && segment.getTargetDiameter() == null) {
                throw new BusinessException("改直径或分层复卷必须填写目标直径");
            }
            if ((rewindMode == 2 || rewindMode == 3) && segment.getFinishCoreDiameter() == null) {
                throw new BusinessException("改直径或分层复卷必须填写成品纸芯");
            }
            int repeatCount = segment.getRepeatCount() == null ? 1 : segment.getRepeatCount();
            if (repeatCount < 1) {
                throw new BusinessException("分段重复次数至少为1");
            }
            for (RewindPlanPreviewDTO.RewindLayoutItemDTO item : segment.getLayoutItems()) {
                String itemType = resolveLayoutItemType(item);
                if (!LAYOUT_ITEM_FINISH.equals(itemType) && !LAYOUT_ITEM_TRIM.equals(itemType)) {
                    throw new BusinessException("门幅排布类型只能是成品或修边");
                }
                int quantity = item.getQuantity() == null ? 1 : item.getQuantity();
                if (quantity < 1) {
                    throw new BusinessException("门幅排布数量至少为1");
                }
                if (rewindMode == 4 && LAYOUT_ITEM_FINISH.equals(itemType)) {
                    validateLayoutLayers(item);
                }
            }
        }
    }

    private void validateSameSpecRewind(RewindPlanPreviewDTO dto, OriginalRoll roll) {
        if (!Integer.valueOf(6).equals(dto.getRewindMode())) return;
        if (dto.getSegments() == null || dto.getSegments().size() != 1) {
            throw new BusinessException("同规格复卷只能配置一个加工分段");
        }
        RewindPlanPreviewDTO.RewindSegmentDTO segment = dto.getSegments().getFirst();
        if (!Integer.valueOf(1).equals(segment.getRepeatCount())) {
            throw new BusinessException("同规格复卷不能拆分或重复产出");
        }
        List<RewindPlanPreviewDTO.RewindLayoutItemDTO> items = segment.getLayoutItems();
        if (items == null || items.size() != 1
                || !LAYOUT_ITEM_FINISH.equals(resolveLayoutItemType(items.getFirst()))) {
            throw new BusinessException("同规格复卷必须且只能配置一个正式成品");
        }
        RewindPlanPreviewDTO.RewindLayoutItemDTO item = items.getFirst();
        if (!Integer.valueOf(1).equals(item.getQuantity())) {
            throw new BusinessException("同规格复卷只能一卷产出一卷");
        }
        requireSameValue("门幅", item.getWidth(), effectiveRollWidth(roll));
        requireSameValue("直径", segment.getTargetDiameter(), roll.getOriginalDiameter());
        requireSameValue("纸芯", segment.getFinishCoreDiameter(), roll.getCoreDiameter());
    }

    private void requireSameValue(String label, Integer output, Integer source) {
        if (!Objects.equals(output, source)) {
            throw new BusinessException("同规格复卷的" + label + "必须与母卷一致");
        }
    }

    private Integer effectiveRollWidth(OriginalRoll roll) {
        return roll.getActualWidth() != null && roll.getActualWidth() > 0
                ? roll.getActualWidth() : roll.getOriginalWidth();
    }

    private void validateLayoutLayers(RewindPlanPreviewDTO.RewindLayoutItemDTO item) {
        if (item.getLayers() == null || item.getLayers().isEmpty()) {
            throw new BusinessException("内外层分层模式下，每个成品排布必须填写分层参数");
        }
        for (FinishConfigSpecDTO.FinishLayerDTO layer : item.getLayers()) {
            if (layer.getOutDiameter() == null || layer.getOutDiameter() <= 0
                    || layer.getCoreDiameter() == null || layer.getCoreDiameter() <= 0) {
                throw new BusinessException("分层外径和纸芯必须大于0");
            }
        }
    }

    private FinishPreviewVO buildRewindPreview(String orderUuid, OriginalRoll roll, RewindPlanPreviewDTO dto) {
        List<PreviewPiece> pieces = new ArrayList<>();
        List<FinishPreviewVO.SegmentPreview> segmentPreviews = new ArrayList<>();
        int originalWidth = roll.getOriginalWidth() == null ? 0 : roll.getOriginalWidth();
        Map<String, OriginalRoll> sourceRolls = dto.getRewindMode() != null && dto.getRewindMode() == 5
                ? orderRollMap(orderUuid)
                : Map.of();
        boolean consumptionPlan = dto.getRewindMode() != null && dto.getRewindMode() == 5
                && MultiSourceConsumptionNormalizer.hasConsumption(dto.getSegments());
        BigDecimal totalWeight = consumptionPlan
                ? MultiSourceConsumptionNormalizer.totalConsumedWeight(dto.getSegments(), sourceRolls)
                : previewTotalWeight(roll, dto, sourceRolls);
        BigDecimal ratioTotal = consumptionPlan ? totalWeight : dto.getSegments().stream()
                .map(segment -> segment.getSegmentRatio() == null ? BigDecimal.ONE : segment.getSegmentRatio())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal weightedTrimWidth = BigDecimal.ZERO;
        int trimCount = 0;
        int fallbackSort = 1;
        for (RewindPlanPreviewDTO.RewindSegmentDTO segment : dto.getSegments()) {
            int repeatCount = segment.getRepeatCount() == null ? 1 : segment.getRepeatCount();
            BigDecimal segmentRatio = consumptionPlan
                    ? resolveConsumedSegmentRatio(segment, sourceRolls, ratioTotal)
                    : resolveSegmentRatio(segment, ratioTotal);
            int layoutWidth = calcLayoutWidth(segment, LAYOUT_ITEM_FINISH);
            int trimWidth = calcTrimWidth(dto.getRewindMode(), originalWidth, segment);
            String sourceSummary = sourceSummary(dto.getRewindMode(), segment, sourceRolls);
            FinishPreviewVO.SegmentPreview segmentPreview = new FinishPreviewVO.SegmentPreview();
            segmentPreview.setSegmentSort(segment.getSegmentSort() == null ? fallbackSort : segment.getSegmentSort());
            segmentPreview.setSegmentRatio(segmentRatio);
            segmentPreview.setTargetDiameter(segment.getTargetDiameter());
            segmentPreview.setRepeatCount(repeatCount);
            segmentPreview.setLayoutWidth(layoutWidth);
            segmentPreview.setTrimWidth(trimWidth);
            segmentPreview.setTrimWeight(calcSegmentTrimWeight(totalWeight, trimWidth, segmentRatio, originalWidth));
            segmentPreview.setSummary(buildSegmentSummary(segment));
            segmentPreviews.add(segmentPreview);

            BigDecimal repeatedSegmentRatio = segmentRatio.divide(BigDecimal.valueOf(repeatCount), 6, RoundingMode.HALF_UP);
            for (int repeat = 0; repeat < repeatCount; repeat++) {
                List<PreviewPiece> repeatPieces = new ArrayList<>();
                for (RewindPlanPreviewDTO.RewindLayoutItemDTO item : segment.getLayoutItems()) {
                    if (!LAYOUT_ITEM_FINISH.equals(resolveLayoutItemType(item))) {
                        continue;
                    }
                    int quantity = item.getQuantity() == null ? 1 : item.getQuantity();
                    for (int i = 0; i < quantity; i++) {
                        PreviewPiece piece = new PreviewPiece();
                        piece.segmentSort = segmentPreview.getSegmentSort();
                        piece.segmentRatio = repeatedSegmentRatio;
                        piece.finishWidth = item.getWidth();
                        piece.finishDiameter = previewFinishDiameter(segment, item);
                        piece.finishCoreDiameter = previewFinishCoreDiameter(segment, item);
                        piece.customerPaperName = item.getCustomerPaperName();
                        piece.customerGramWeight = item.getCustomerGramWeight();
                        piece.customerFinishWidth = item.getCustomerFinishWidth();
                        piece.customerSpecOverrideReason = item.getCustomerSpecOverrideReason();
                        piece.originalWidth = originalWidth;
                        piece.trimWidth = trimWidth;
                        piece.sourceSummary = sourceSummary;
                        piece.layers = item.getLayers();
                        piece.basis = previewBasis(roll, dto.getRewindMode(), segment, item, repeatedSegmentRatio);
                        repeatPieces.add(piece);
                    }
                }
                if (!repeatPieces.isEmpty() && trimWidth > 0) {
                    weightedTrimWidth = weightedTrimWidth.add(BigDecimal.valueOf(trimWidth).multiply(repeatedSegmentRatio));
                    trimCount++;
                }
                pieces.addAll(repeatPieces);
            }
            fallbackSort++;
        }

        List<FinishPreviewVO.FinishItemPreview> finishes = allocatePreviewWeights(
                pieces, totalWeight, weightedTrimWidth, BigDecimal.valueOf(originalWidth));
        FinishPreviewVO vo = new FinishPreviewVO();
        vo.setOriginalUuid(roll.getUuid());
        vo.setRewindMode(dto.getRewindMode());
        vo.setOriginalWidth(roll.getOriginalWidth());
        vo.setFinishCount(finishes.size());
        vo.setTrimCount(trimCount);
        vo.setSpareCount(dto.getSpareCount() == null ? 0 : dto.getSpareCount());
        vo.setTotalEstimateWeight(finishes.stream()
                .map(FinishPreviewVO.FinishItemPreview::getEstimateWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        vo.setTotalTrimWeight(segmentPreviews.stream()
                .map(FinishPreviewVO.SegmentPreview::getTrimWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        vo.setSegments(segmentPreviews);
        vo.setFinishes(finishes);
        return vo;
    }

    private BigDecimal calcSegmentTrimWeight(BigDecimal totalWeight, int trimWidth, BigDecimal segmentRatio,
                                             int originalWidth) {
        if (trimWidth <= 0 || originalWidth <= 0 || totalWeight == null || totalWeight.signum() <= 0) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
        BigDecimal ratio = segmentRatio == null ? BigDecimal.ZERO : segmentRatio;
        return totalWeight.multiply(BigDecimal.valueOf(trimWidth)).multiply(ratio)
                .divide(BigDecimal.valueOf(originalWidth), 3, RoundingMode.HALF_UP);
    }

    private List<FinishPreviewVO.FinishItemPreview> allocatePreviewWeights(List<PreviewPiece> pieces,
                                                                           BigDecimal totalWeight,
                                                                           BigDecimal trimTotalWidth,
                                                                           BigDecimal originalWidth) {
        List<RewindWeightCalculator.PieceInput> inputs = new ArrayList<>(pieces.size());
        for (PreviewPiece piece : pieces) {
            inputs.add(new RewindWeightCalculator.PieceInput(piece.basis, null));
        }
        List<RewindWeightCalculator.PieceResult> results = RewindWeightCalculator.allocate(
                totalWeight, inputs, trimTotalWidth, originalWidth, BigDecimal.ZERO);
        List<FinishPreviewVO.FinishItemPreview> previews = new ArrayList<>(pieces.size());
        for (int i = 0; i < pieces.size(); i++) {
            PreviewPiece piece = pieces.get(i);
            RewindWeightCalculator.PieceResult result = results.get(i);
            FinishPreviewVO.FinishItemPreview preview = new FinishPreviewVO.FinishItemPreview();
            preview.setSegmentSort(piece.segmentSort);
            preview.setFinishWidth(piece.finishWidth);
            preview.setFinishDiameter(piece.finishDiameter);
            preview.setFinishCoreDiameter(piece.finishCoreDiameter);
            preview.setCustomerPaperName(piece.customerPaperName);
            preview.setCustomerGramWeight(piece.customerGramWeight);
            preview.setCustomerFinishWidth(piece.customerFinishWidth);
            preview.setCustomerSpecOverrideReason(piece.customerSpecOverrideReason);
            preview.setSegmentRatio(piece.segmentRatio);
            preview.setEstimateWeight(result.weight);
            preview.setTrimWidth(piece.trimWidth);
            preview.setTrimWeight(result.trimWeightShare);
            preview.setSourceSummary(piece.sourceSummary);
            preview.setLayers(piece.layers);
            previews.add(preview);
        }
        return previews;
    }

    private Map<String, OriginalRoll> orderRollMap(String orderUuid) {
        List<OriginalRoll> orderRolls = originalRollMapper.selectList(new LambdaQueryWrapper<OriginalRoll>()
                .eq(OriginalRoll::getOrderUuid, orderUuid));
        Map<String, OriginalRoll> rollByUuid = new LinkedHashMap<>();
        for (OriginalRoll sourceRoll : orderRolls) {
            rollByUuid.put(sourceRoll.getUuid(), sourceRoll);
        }
        return rollByUuid;
    }

    private BigDecimal previewTotalWeight(OriginalRoll roll, RewindPlanPreviewDTO dto,
                                          Map<String, OriginalRoll> sourceRolls) {
        if (dto.getRewindMode() == null || dto.getRewindMode() != 5) {
            return calcTotalWeight(roll.getRollWeight(), roll.getPieceNum());
        }
        Set<String> sourceUuids = new LinkedHashSet<>();
        for (RewindPlanPreviewDTO.RewindSegmentDTO segment : dto.getSegments()) {
            if (segment.getSources() == null) {
                continue;
            }
            for (FinishConfigSpecDTO.FinishSourceDTO source : segment.getSources()) {
                sourceUuids.add(source.getOriginalUuid());
            }
        }
        BigDecimal total = BigDecimal.ZERO;
        for (String sourceUuid : sourceUuids) {
            OriginalRoll sourceRoll = sourceRolls.get(sourceUuid);
            if (sourceRoll != null) {
                total = total.add(calcTotalWeight(sourceRoll.getRollWeight(), sourceRoll.getPieceNum()));
            }
        }
        return total.signum() == 0 ? calcTotalWeight(roll.getRollWeight(), roll.getPieceNum()) : total;
    }

    private String sourceSummary(Integer rewindMode, RewindPlanPreviewDTO.RewindSegmentDTO segment,
                                 Map<String, OriginalRoll> sourceRolls) {
        if (rewindMode == null || rewindMode != 5) {
            return "当前母卷";
        }
        if (segment.getSources() == null || segment.getSources().isEmpty()) {
            return "未选择来源母卷";
        }
        List<String> parts = new ArrayList<>();
        for (FinishConfigSpecDTO.FinishSourceDTO source : segment.getSources()) {
            OriginalRoll sourceRoll = sourceRolls.get(source.getOriginalUuid());
            String label = sourceRoll == null ? source.getOriginalUuid() : sourceRollLabel(sourceRoll);
            String ratio = source.getShareRatio() == null ? "0" : source.getShareRatio().stripTrailingZeros().toPlainString();
            if (source.getConsumeRatio() == null) {
                parts.add(label + " 本段" + ratio + "%");
            } else {
                String consume = source.getConsumeRatio().stripTrailingZeros().toPlainString();
                parts.add(label + " 消耗" + consume + "%/本段" + ratio + "%");
            }
        }
        return "接纸：" + String.join(" → ", parts);
    }

    private String sourceRollLabel(OriginalRoll roll) {
        if (StringUtils.hasText(roll.getRollNo())) {
            return roll.getRollNo();
        }
        if (StringUtils.hasText(roll.getExtraNo())) {
            return roll.getExtraNo();
        }
        return roll.getUuid();
    }

    private Integer previewFinishDiameter(RewindPlanPreviewDTO.RewindSegmentDTO segment,
                                          RewindPlanPreviewDTO.RewindLayoutItemDTO item) {
        if (segment.getTargetDiameter() != null) {
            return segment.getTargetDiameter();
        }
        return maxLayerOutDiameter(item.getLayers());
    }

    private Integer previewFinishCoreDiameter(RewindPlanPreviewDTO.RewindSegmentDTO segment,
                                              RewindPlanPreviewDTO.RewindLayoutItemDTO item) {
        if (segment.getFinishCoreDiameter() != null) {
            return segment.getFinishCoreDiameter();
        }
        return firstLayerCoreDiameter(item.getLayers());
    }

    private BigDecimal previewBasis(OriginalRoll roll, Integer rewindMode, RewindPlanPreviewDTO.RewindSegmentDTO segment,
                                    RewindPlanPreviewDTO.RewindLayoutItemDTO item, BigDecimal segmentRatio) {
        if (rewindMode == null || rewindMode == 1 || rewindMode == 6) {
            return BigDecimal.valueOf(item.getWidth()).multiply(segmentRatio);
        }
        if (rewindMode == 2) {
            return calcLayerArea(segment.getTargetDiameter(), segment.getFinishCoreDiameter()).multiply(segmentRatio);
        }
        if (rewindMode == 5) {
            return BigDecimal.valueOf(item.getWidth()).multiply(segmentRatio);
        }
        if (rewindMode == 4 && item.getLayers() != null && !item.getLayers().isEmpty()) {
            return layoutLayerArea(item.getLayers()).multiply(segmentRatio);
        }
        if (rewindMode == 3 || rewindMode == 4) {
            BigDecimal originalWidth = roll.getOriginalWidth() == null ? BigDecimal.ZERO : BigDecimal.valueOf(roll.getOriginalWidth());
            if (originalWidth.signum() == 0) {
                return calcLayerArea(segment.getTargetDiameter(), segment.getFinishCoreDiameter()).multiply(segmentRatio);
            }
            return calcLayerArea(segment.getTargetDiameter(), segment.getFinishCoreDiameter())
                    .multiply(BigDecimal.valueOf(item.getWidth()).divide(originalWidth, 6, RoundingMode.HALF_UP))
                    .multiply(segmentRatio);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal resolveSegmentRatio(RewindPlanPreviewDTO.RewindSegmentDTO segment, BigDecimal ratioTotal) {
        BigDecimal ratio = segment.getSegmentRatio() == null ? BigDecimal.ONE : segment.getSegmentRatio();
        if (ratioTotal.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return ratio.divide(ratioTotal, 6, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveConsumedSegmentRatio(RewindPlanPreviewDTO.RewindSegmentDTO segment,
                                                   Map<String, OriginalRoll> sourceRolls,
                                                   BigDecimal totalWeight) {
        if (totalWeight.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return MultiSourceConsumptionNormalizer.segmentConsumedWeight(segment, sourceRolls)
                .divide(totalWeight, 6, RoundingMode.HALF_UP);
    }

    private int calcLayoutWidth(RewindPlanPreviewDTO.RewindSegmentDTO segment, String itemType) {
        int width = 0;
        for (RewindPlanPreviewDTO.RewindLayoutItemDTO item : segment.getLayoutItems()) {
            if (itemType.equals(resolveLayoutItemType(item))) {
                width += item.getWidth() * (item.getQuantity() == null ? 1 : item.getQuantity());
            }
        }
        return width;
    }

    private int calcTrimWidth(Integer rewindMode, int originalWidth, RewindPlanPreviewDTO.RewindSegmentDTO segment) {
        if (segment.getLayoutItems() == null || segment.getLayoutItems().isEmpty()) {
            return 0;
        }
        if (rewindMode != null && rewindMode == 2) {
            if (calcLayoutWidth(segment, LAYOUT_ITEM_TRIM) > 0) {
                throw new BusinessException("改直径不变门幅模式不能配置修边");
            }
            return 0;
        }
        int explicitTrim = calcLayoutWidth(segment, LAYOUT_ITEM_TRIM);
        if (explicitTrim > 0) {
            int totalWidth = calcLayoutWidth(segment, LAYOUT_ITEM_FINISH) + explicitTrim;
            if (originalWidth > 0 && totalWidth > originalWidth) {
                throw new BusinessException("门幅排布加修边宽度不能超过原纸门幅");
            }
            return explicitTrim;
        }
        int finishWidth = calcLayoutWidth(segment, LAYOUT_ITEM_FINISH);
        if (originalWidth > 0 && finishWidth > originalWidth) {
            throw new BusinessException("门幅排布宽度不能超过原纸门幅");
        }
        return Math.max(0, originalWidth - finishWidth);
    }

    private String buildSegmentSummary(RewindPlanPreviewDTO.RewindSegmentDTO segment) {
        List<String> parts = new ArrayList<>();
        for (RewindPlanPreviewDTO.RewindLayoutItemDTO item : segment.getLayoutItems()) {
            String suffix = LAYOUT_ITEM_TRIM.equals(resolveLayoutItemType(item)) ? "修边" : "成品";
            int quantity = item.getQuantity() == null ? 1 : item.getQuantity();
            parts.add(item.getWidth() + "mm×" + quantity + suffix);
        }
        return String.join(" + ", parts);
    }

    private String resolveLayoutItemType(RewindPlanPreviewDTO.RewindLayoutItemDTO item) {
        return StringUtils.hasText(item.getItemType()) ? item.getItemType().trim().toUpperCase() : LAYOUT_ITEM_FINISH;
    }

    private Integer maxLayerOutDiameter(List<FinishConfigSpecDTO.FinishLayerDTO> layers) {
        if (layers == null || layers.isEmpty()) {
            return null;
        }
        return layers.stream()
                .map(FinishConfigSpecDTO.FinishLayerDTO::getOutDiameter)
                .filter(value -> value != null && value > 0)
                .max(Integer::compareTo)
                .orElse(null);
    }

    private Integer firstLayerCoreDiameter(List<FinishConfigSpecDTO.FinishLayerDTO> layers) {
        if (layers == null || layers.isEmpty()) {
            return null;
        }
        return layers.stream()
                .map(FinishConfigSpecDTO.FinishLayerDTO::getCoreDiameter)
                .filter(value -> value != null && value > 0)
                .findFirst()
                .orElse(null);
    }

    private static final class PreviewPiece {
        private Integer segmentSort;
        private BigDecimal segmentRatio;
        private Integer finishWidth;
        private Integer finishDiameter;
        private Integer finishCoreDiameter;
        private String customerPaperName;
        private Integer customerGramWeight;
        private Integer customerFinishWidth;
        private String customerSpecOverrideReason;
        private Integer originalWidth;
        private Integer trimWidth;
        private String sourceSummary = "当前母卷";
        private List<FinishConfigSpecDTO.FinishLayerDTO> layers = List.of();
        private BigDecimal basis = BigDecimal.ZERO;
    }

    private void validateFinishConfig(String orderUuid, OriginalRoll roll, FinishConfigSaveDTO dto) {
        if (!requiresMainProcess(roll)) {
            return;
        }
        if (isOnSite(roll)) {
            validateOnSiteConfig(dto);
            return;
        }
        if (dto.getFinishSpecs() == null || dto.getFinishSpecs().isEmpty()) {
            if (roll.getMainStepType() != FeeCalculator.STEP_TYPE_REWIND
                    || dto.getRewindSegments() == null || dto.getRewindSegments().isEmpty()) {
                throw new BusinessException("成品规格不能为空");
            }
        }
        if (roll.getMainStepType() == FeeCalculator.STEP_TYPE_REWIND) {
            validateRewindConfig(orderUuid, roll, dto);
            validateFinishSpecBasics(roll, dto.getFinishSpecs());
            return;
        }
        validateSawConfig(roll, dto);
    }

    private void validateOnSiteConfig(FinishConfigSaveDTO dto) {
        for (FinishConfigSpecDTO spec : dto.getFinishSpecs() == null ? List.<FinishConfigSpecDTO>of() : dto.getFinishSpecs()) {
            validateSpecType(spec);
            validateSpecCount(spec);
        }
    }

    private void validateFinishSpecBasics(OriginalRoll roll, List<FinishConfigSpecDTO> specs) {
        for (FinishConfigSpecDTO spec : specs == null ? List.<FinishConfigSpecDTO>of() : specs) {
            validateSpecType(spec);
            validateSpecCount(spec);
            if (roll.getProcessMode() != null && roll.getProcessMode() == DEFAULT_PROCESS_MODE
                    && (spec.getFinishWidth() == null || spec.getFinishWidth() < 1)) {
                throw new BusinessException("标准加工成品门幅必须大于0");
            }
        }
    }

    private void validateSawConfig(OriginalRoll roll, FinishConfigSaveDTO dto) {
        int finishCount = 0;
        int usedWidth = 0;
        for (FinishConfigSpecDTO spec : dto.getFinishSpecs() == null ? List.<FinishConfigSpecDTO>of() : dto.getFinishSpecs()) {
            validateSpecType(spec);
            validateSpecCount(spec);
            if (!isOnSite(roll) && (spec.getFinishWidth() == null || spec.getFinishWidth() < 1)) {
                throw new BusinessException("锯纸成品/切边门幅必须大于0");
            }
            usedWidth += (spec.getFinishWidth() == null ? 0 : spec.getFinishWidth()) * safeCount(spec);
            if (!isTrimSpec(spec)) {
                finishCount += safeCount(spec);
            }
        }
        if (finishCount < 1) {
            throw new BusinessException("锯纸至少需要一条成品规格，切边不能生成成品卷号");
        }
        if (roll.getOriginalWidth() != null && roll.getOriginalWidth() > 0 && usedWidth > roll.getOriginalWidth()) {
            throw new BusinessException("锯纸成品门幅加切边不能超过母卷门幅");
        }
        sawPlanPreviewer.calculate(dto.getFinishSpecs(), roll, dto.getWidthDifferencePolicy());
    }

    private void validateSpecType(FinishConfigSpecDTO spec) {
        String itemType = resolveSpecItemType(spec);
        if (!LAYOUT_ITEM_FINISH.equals(itemType) && !LAYOUT_ITEM_TRIM.equals(itemType)) {
            throw new BusinessException("规格类型只能是成品或切边");
        }
    }

    private void validateSpecCount(FinishConfigSpecDTO spec) {
        if (spec.getCount() == null || spec.getCount() < 1) {
            throw new BusinessException("成品/切边规格数量至少为1");
        }
    }

    private void validateRewindConfig(String orderUuid, OriginalRoll roll, FinishConfigSaveDTO dto) {
        Integer rewindMode = dto.getRewindMode();
        if (rewindMode == null) {
            throw new BusinessException("复卷模式不能为空");
        }
        if (rewindMode < 1 || rewindMode > 6) {
            throw new BusinessException("复卷模式只能选择1到6类");
        }
        if (dto.getRewindSegments() != null && !dto.getRewindSegments().isEmpty()) {
            RewindPlanPreviewDTO previewDto = new RewindPlanPreviewDTO();
            previewDto.setRewindMode(rewindMode);
            previewDto.setSpareCount(dto.getSpareCount());
            previewDto.setSegments(dto.getRewindSegments());
            validateRewindPreviewPlan(previewDto);
            validateSameSpecRewind(previewDto, roll);
            if (rewindMode == 5) {
                validateRewindSegmentSources(dto.getRewindSegments(), orderRollMap(orderUuid));
            }
            return;
        }
        if (rewindMode == 6) {
            throw new BusinessException("同规格复卷必须保存一卷进、一卷出的完整方案");
        }
        if (rewindMode == 4) {
            for (FinishConfigSpecDTO spec : dto.getFinishSpecs()) {
                if (isTrimSpec(spec)) {
                    continue;
                }
                if (spec.getLayers() == null || spec.getLayers().isEmpty()) {
                    throw new BusinessException("内外层分层模式必须填写分层参数");
                }
            }
        }
        if (rewindMode == 5) {
            List<OriginalRoll> orderRolls = originalRollMapper.selectList(new LambdaQueryWrapper<OriginalRoll>()
                    .eq(OriginalRoll::getOrderUuid, orderUuid));
            Map<String, OriginalRoll> rollByUuid = new LinkedHashMap<>();
            for (OriginalRoll sourceRoll : orderRolls) {
                rollByUuid.put(sourceRoll.getUuid(), sourceRoll);
            }
            for (FinishConfigSpecDTO spec : dto.getFinishSpecs()) {
                if (isTrimSpec(spec)) {
                    continue;
                }
                if (spec.getSources() == null || spec.getSources().isEmpty()) {
                    throw new BusinessException("多母卷合并复卷必须选择来源原纸");
                }
                BigDecimal totalRatio = BigDecimal.ZERO;
                for (FinishConfigSpecDTO.FinishSourceDTO source : spec.getSources()) {
                    if (!StringUtils.hasText(source.getOriginalUuid()) || !rollByUuid.containsKey(source.getOriginalUuid())) {
                        throw new BusinessException("多母卷合并复卷来源原纸不存在");
                    }
                    BigDecimal ratio = source.getShareRatio();
                    if (ratio == null || ratio.signum() <= 0) {
                        throw new BusinessException("多母卷合并复卷必须填写分摊比例");
                    }
                    totalRatio = totalRatio.add(ratio);
                }
                if (totalRatio.compareTo(new BigDecimal("100.00")) != 0) {
                    throw new BusinessException("多母卷合并复卷分摊比例合计必须等于100%");
                }
            }
        }
    }

    private void validateRewindSegmentSources(List<RewindPlanPreviewDTO.RewindSegmentDTO> segments,
                                              Map<String, OriginalRoll> rollByUuid) {
        Set<String> rollUuids = new LinkedHashSet<>(rollByUuid.keySet());
        boolean consumptionPlan = MultiSourceConsumptionNormalizer.hasConsumption(segments);
        for (RewindPlanPreviewDTO.RewindSegmentDTO segment : segments) {
            if (segment.getSources() == null || segment.getSources().isEmpty()) {
                throw new BusinessException("多母卷合并复卷每个分段必须选择来源原纸");
            }
            BigDecimal totalRatio = BigDecimal.ZERO;
            for (FinishConfigSpecDTO.FinishSourceDTO source : segment.getSources()) {
                if (!StringUtils.hasText(source.getOriginalUuid()) || !rollUuids.contains(source.getOriginalUuid())) {
                    throw new BusinessException("多母卷合并复卷来源原纸不存在");
                }
                if (consumptionPlan) {
                    if (source.getConsumeRatio() == null || source.getConsumeRatio().signum() <= 0) {
                        throw new BusinessException("多母卷合并复卷必须填写来源消耗比例");
                    }
                    continue;
                }
                BigDecimal ratio = source.getShareRatio();
                if (ratio == null || ratio.signum() <= 0) {
                    throw new BusinessException("多母卷合并复卷必须填写分摊比例");
                }
                totalRatio = totalRatio.add(ratio);
            }
            if (!consumptionPlan && totalRatio.compareTo(new BigDecimal("100.00")) != 0) {
                throw new BusinessException("多母卷合并复卷分摊比例合计必须等于100%");
            }
        }
        MultiSourceConsumptionNormalizer.normalize(segments, rollByUuid);
    }

    private void voidExistingFinishConfig(String orderUuid, OriginalRoll roll) {
        List<FinishRoll> existing = finishRollMapper.selectList(
                new LambdaQueryWrapper<FinishRoll>()
                        .eq(FinishRoll::getOrderUuid, orderUuid)
                        .eq(FinishRoll::getOriginalRollNos, finishOriginalKey(roll))
                        .ne(FinishRoll::getRollNoStatus, ROLL_NO_VOID));
        for (FinishRoll finish : existing) {
            if (finish.getActualWeight() != null) {
                throw new BusinessException("已有回录重量的成品不可重新配置");
            }
        }
        if (!existing.isEmpty()) {
            finishOriginalRelMapper.delete(new LambdaQueryWrapper<FinishOriginalRel>()
                    .in(FinishOriginalRel::getFinishUuid, existing.stream().map(FinishRoll::getUuid).toList()));
        }
        for (FinishRoll finish : existing) {
            finish.setRollNoStatus(ROLL_NO_VOID);
            finishRollMapper.updateById(finish);
        }
    }

    private ProcessStep syncMainStep(ProcessOrder order, OriginalRoll roll, FinishConfigSaveDTO dto) {
        ProcessStep mainStep = processStepMapper.selectOne(
                new LambdaQueryWrapper<ProcessStep>()
                        .eq(ProcessStep::getOriginalUuid, roll.getUuid())
                        .eq(ProcessStep::getIsMain, STEP_MAIN)
                        .last("LIMIT 1"));
        if (!requiresMainProcess(roll)) {
            if (mainStep != null) {
                processStepMapper.deleteById(mainStep.getUuid());
            }
            return null;
        }
        if (mainStep == null) {
            mainStep = new ProcessStep();
            mainStep.setOrderUuid(order.getUuid());
            mainStep.setOriginalUuid(roll.getUuid());
            mainStep.setStepSort(1);
            mainStep.setIsMain(STEP_MAIN);
        }
        mainStep.setStepType(roll.getMainStepType());
        mainStep.setStepName(stepName(roll.getMainStepType()));
        applyMachine(mainStep, dto.getMachineUuid(), roll.getMachineUuid());
        mainStep.setKnifeCount(resolveKnifeCount(roll, dto));
        mainStep.setProcessWeight(mainStep.getStepType() == FeeCalculator.STEP_TYPE_REWIND
                ? rewindProcessWeight(order.getUuid(), roll, dto)
                : null);
        mainStep.setUnitPrice(dto.getUnitPrice());
        applySawWidthDifference(mainStep, roll, dto);
        mainStep.setRemark(rewindRemark(dto));
        processCatalogStepValidator.validate(mainStep, roll);
        if (mainStep.getUuid() == null) {
            processStepMapper.insert(mainStep);
        } else {
            processStepMapper.updateById(mainStep);
        }
        return mainStep;
    }

    private BigDecimal rewindProcessWeight(String orderUuid, OriginalRoll roll, FinishConfigSaveDTO dto) {
        BigDecimal totalWeight = calcTotalWeight(roll.getRollWeight(), roll.getPieceNum());
        if (dto.getRewindMode() != null && dto.getRewindMode() == 5) {
            totalWeight = multiSourceProcessWeight(orderUuid, roll, dto);
        }
        return totalWeight.divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
    }

    private BigDecimal multiSourceProcessWeight(String orderUuid, OriginalRoll roll, FinishConfigSaveDTO dto) {
        if (dto.getRewindSegments() != null && !dto.getRewindSegments().isEmpty()) {
            Map<String, OriginalRoll> sourceRolls = orderRollMap(orderUuid);
            if (MultiSourceConsumptionNormalizer.hasConsumption(dto.getRewindSegments())) {
                return MultiSourceConsumptionNormalizer.totalConsumedWeight(dto.getRewindSegments(), sourceRolls);
            }
            RewindPlanPreviewDTO previewDto = new RewindPlanPreviewDTO();
            previewDto.setRewindMode(dto.getRewindMode());
            previewDto.setSegments(dto.getRewindSegments());
            return previewTotalWeight(roll, previewDto, sourceRolls);
        }
        if (dto.getFinishSpecs() != null && !dto.getFinishSpecs().isEmpty()) {
            return calcSourceTotalWeight(orderUuid, dto.getFinishSpecs());
        }
        return calcTotalWeight(roll.getRollWeight(), roll.getPieceNum());
    }

    private List<FinishConfigSpecDTO> buildSawSaveSpecs(OriginalRoll roll, FinishConfigSaveDTO dto) {
        if (isOnSite(roll)) {
            return buildOnSiteSaveSpecs(dto.getFinishSpecs());
        }
        return sawPlanPreviewer.saveSpecs(dto.getFinishSpecs(), roll, dto.getWidthDifferencePolicy());
    }

    private Integer resolveKnifeCount(OriginalRoll roll, FinishConfigSaveDTO dto) {
        if (roll.getMainStepType() == null || roll.getMainStepType() != FeeCalculator.STEP_TYPE_SAW) {
            return dto.getKnifeCount();
        }
        if (isOnSite(roll)) {
            return dto.getKnifeCount();
        }
        SawPlanCalculation calculation = sawPlanPreviewer.calculate(
                dto.getFinishSpecs(), roll, dto.getWidthDifferencePolicy());
        int derived = calculation.knifeCount();
        return derived > 0 ? derived : dto.getKnifeCount();
    }

    private void applySawWidthDifference(ProcessStep step, OriginalRoll roll, FinishConfigSaveDTO dto) {
        if (!Integer.valueOf(FeeCalculator.STEP_TYPE_SAW).equals(step.getStepType())) {
            step.setWidthDifferencePolicy(null);
            step.setPlannedLossWidth(null);
            step.setPlannedLossWeight(null);
            return;
        }
        SawPlanCalculation calculation = sawPlanPreviewer.calculate(
                dto.getFinishSpecs(), roll, dto.getWidthDifferencePolicy());
        step.setWidthDifferencePolicy(calculation.policy().name());
        step.setPlannedLossWidth(calculation.policy() == com.paper.mes.processorder.model.WidthDifferencePolicy.LOSS
                ? calculation.differenceWidth() : 0);
        step.setPlannedLossWeight(calculation.policy() == com.paper.mes.processorder.model.WidthDifferencePolicy.LOSS
                ? calculation.differenceWeight() : BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
    }

    private List<FinishConfigSpecDTO> buildOnSiteSaveSpecs(List<FinishConfigSpecDTO> specs) {
        return List.of();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FeeResultVO adjustProcessStepPricing(String stepUuid, ProcessStepPricingAdjustmentDTO dto) {
        ProcessStep existing = processStepMapper.selectById(stepUuid);
        if (existing == null) {
            throw new BusinessException(ErrorCode.E002, "工序不存在");
        }
        businessLockService.lockProcessOrders(List.of(existing.getOrderUuid()));
        ProcessStep step = processStepMapper.selectById(stepUuid);
        if (step == null) {
            throw new BusinessException(ErrorCode.E006, "工序已被其他操作删除，请刷新后重试");
        }
        ProcessOrder order = requireOrder(step.getOrderUuid());
        validatePricingAdjustmentStatus(order);
        ensureOrderNotReferencedBySettle(order.getUuid(), "加工单已生成结算单，请先作废结算单再调整服务费");
        applyPricingAdjustment(step, dto);
        ConcurrencyGuard.requireRowUpdated(processStepMapper.updateById(step));
        FeeResultVO result = calcFee(order.getUuid());
        requirePricingApprovalIfNeeded(result);
        operationLogService.record(OperationLogService.BIZ_TYPE_ORDER, order.getUuid(), order.getOrderNo(),
                OperationLogService.ACTION_PRICING_ADJUST, currentOperator(),
                "工序 " + stepUuid + " 计价模式调整为 " + dto.getBillingMode() + "：" + dto.getReason().trim());
        return result;
    }

    private void requirePricingApprovalIfNeeded(FeeResultVO result) {
        if (result.getStepFees() == null) return;
        BigDecimal discount = result.getStepFees().stream()
                .map(FeeResultVO.StepFee::getPricingAdjustmentAmount)
                .filter(amount -> amount != null && amount.signum() < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (discount.compareTo(processStepPricingSettings.autoApproveLimit()) <= 0) return;
        try {
            permissionChecker.require(Permissions.ORDER_PRICING_APPROVE);
        } catch (BusinessException ex) {
            if (ex.getCode() != ResultCode.FORBIDDEN) throw ex;
            throw new BusinessException(ResultCode.FORBIDDEN, ErrorCode.E009.getCode(),
                    ErrorCode.E009.getDefaultMessage());
        }
    }

    private void validatePricingAdjustmentStatus(ProcessOrder order) {
        Integer status = order.getOrderStatus();
        if (status == null || (status != STATUS_TO_RECORD && status != STATUS_FINISHED)) {
            throw new BusinessException(ErrorCode.E001, "仅待回录或已完成加工单可调整计价");
        }
    }

    private void applyPricingAdjustment(ProcessStep step, ProcessStepPricingAdjustmentDTO dto) {
        if (step == null) {
            throw new BusinessException(ErrorCode.E006, "工序已被其他操作删除，请刷新后重试");
        }
        int mode = dto.getBillingMode();
        if (isServiceStep(step.getStepType()) && mode == ProcessStepPricingPolicy.QUANTITY_OVERRIDE) {
            throw new BusinessException("附加工艺数量由系统自动计算，不使用指定数量计价");
        }
        if (isServiceStep(step.getStepType()) && mode == ProcessStepPricingPolicy.STANDARD) {
            requireServiceStandardPricing(dto);
            step.setBillingBasis(dto.getBillingBasis().trim().toUpperCase());
            step.setBillingUnitPrice(dto.getBillingUnitPrice().setScale(4, RoundingMode.HALF_UP));
        }
        if (mode == ProcessStepPricingPolicy.QUANTITY_OVERRIDE) {
            requireBillingQuantity(step, dto.getBillingQuantity());
        }
        if (mode == ProcessStepPricingPolicy.FIXED_AMOUNT && dto.getBillingAmount() == null) {
            throw new BusinessException("固定金额模式必须填写最终金额");
        }
        step.setBillingMode(mode);
        step.setBillingQuantity(mode == ProcessStepPricingPolicy.QUANTITY_OVERRIDE
                ? dto.getBillingQuantity().setScale(3, RoundingMode.HALF_UP) : null);
        step.setBillingAmount(mode == ProcessStepPricingPolicy.FIXED_AMOUNT
                ? dto.getBillingAmount().setScale(2, RoundingMode.HALF_UP)
                : mode == ProcessStepPricingPolicy.FREE ? BigDecimal.ZERO : null);
        step.setPricingAdjustmentReason(dto.getReason().trim());
        step.setPricingAdjustedBy(currentOperator());
        step.setPricingAdjustedAt(LocalDateTime.now());
    }

    private void requireServiceStandardPricing(ProcessStepPricingAdjustmentDTO dto) {
        String basis = dto.getBillingBasis();
        if (!"PIECE".equals(basis) && !"TON".equals(basis)) {
            throw new BusinessException("请选择按件或按吨计费");
        }
        if (dto.getBillingUnitPrice() == null || dto.getBillingUnitPrice().signum() <= 0) {
            throw new BusinessException("附加工艺核定单价必须大于0");
        }
    }

    private void requireBillingQuantity(ProcessStep step, BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessException("最终计费数量必须大于0");
        }
        if (step.getStepType() != null && step.getStepType() == FeeCalculator.STEP_TYPE_SAW) {
            try {
                quantity.toBigIntegerExact();
            } catch (ArithmeticException ex) {
                throw new BusinessException("锯纸最终计费数量必须为整数刀数");
            }
        }
    }

    private boolean isTrimSpec(FinishConfigSpecDTO spec) {
        return LAYOUT_ITEM_TRIM.equals(resolveSpecItemType(spec));
    }

    private String resolveSpecItemType(FinishConfigSpecDTO spec) {
        return StringUtils.hasText(spec.getItemType()) ? spec.getItemType().trim().toUpperCase() : LAYOUT_ITEM_FINISH;
    }

    private int safeCount(FinishConfigSpecDTO spec) {
        return spec.getCount() == null ? 1 : spec.getCount();
    }

    private String rewindRemark(FinishConfigSaveDTO dto) {
        if (dto.getRewindMode() == null) {
            return null;
        }
        String label = switch (dto.getRewindMode()) {
            case 1 -> "改门幅不变直径";
            case 2 -> "改直径不变门幅";
            case 3 -> "改门幅+改直径";
            case 4 -> "内外层分层";
            case 5 -> "多母卷合并复卷";
            default -> "未知";
        };
        return "复卷模式：" + dto.getRewindMode() + "-" + label;
    }

    private void voidExistingRewindConfig(String orderUuid, OriginalRoll roll) {
        processParamMapper.delete(new LambdaQueryWrapper<ProcessParam>()
                .eq(ProcessParam::getOrderUuid, orderUuid)
                .eq(ProcessParam::getOriginalUuid, roll.getUuid()));
    }

    private void saveRewindParams(ProcessOrder order, OriginalRoll roll, ProcessStep mainStep,
                                   FinishConfigSaveDTO dto, List<FinishConfigSpecDTO> specs) {
        Integer rewindMode = dto.getRewindMode();
        if (rewindMode == null || isOnSite(roll)) {
            return;
        }
        int paramMode = rewindMode;
        for (int specIndex = 0; specIndex < specs.size(); specIndex++) {
            FinishConfigSpecDTO spec = specs.get(specIndex);
            if (isTrimSpec(spec)) {
                continue;
            }
            if (rewindMode == 4 && spec.getLayers() != null) {
                for (int layerIndex = 0; layerIndex < spec.getLayers().size(); layerIndex++) {
                    FinishConfigSpecDTO.FinishLayerDTO layer = spec.getLayers().get(layerIndex);
                    ProcessParam param = new ProcessParam();
                    param.setOrderUuid(order.getUuid());
                    param.setOriginalUuid(roll.getUuid());
                    param.setStepUuid(mainStep == null ? null : mainStep.getUuid());
                    param.setParamMode(paramMode);
                    param.setLayerSort(layerIndex + 1);
                    param.setOutDiameter(layer.getOutDiameter());
                    param.setCoreDiameter(layer.getCoreDiameter());
                    param.setLayerWidth(spec.getFinishWidth());
                    param.setAreaValue(calcLayerArea(layer.getOutDiameter(), layer.getCoreDiameter()));
                    param.setAreaRatio(spec.getEstimateWeight());
                    param.setParamJson(toJson(spec));
                    param.setRemark(rewindRemark(dto));
                    processParamMapper.insert(param);
                }
                continue;
            }

            ProcessParam param = new ProcessParam();
            param.setOrderUuid(order.getUuid());
            param.setOriginalUuid(roll.getUuid());
            param.setStepUuid(mainStep == null ? null : mainStep.getUuid());
            param.setParamMode(paramMode);
            param.setLayerSort(specIndex + 1);
            param.setOutDiameter(spec.getFinishDiameter());
            param.setCoreDiameter(spec.getFinishCoreDiameter());
            param.setLayerWidth(spec.getFinishWidth());
            param.setAreaValue(calcSpecArea(rewindMode, spec, roll));
            param.setAreaRatio(spec.getEstimateWeight());
            param.setSplitRatio(spec.getSplitRatio());
            param.setParamJson(toJson(spec));
            param.setRemark(rewindRemark(dto));
            processParamMapper.insert(param);
        }
    }

    private void saveFinishOriginalRelIfNeeded(ProcessOrder order, OriginalRoll roll, FinishConfigSaveDTO dto,
                                              FinishConfigSpecDTO spec, FinishRoll finish) {
        if (dto.getRewindMode() != null && dto.getRewindMode() == 5) {
            List<FinishConfigSpecDTO.FinishSourceDTO> sources = spec.getSources() == null ? List.of() : spec.getSources();
            for (FinishConfigSpecDTO.FinishSourceDTO source : sources) {
                FinishOriginalRel rel = new FinishOriginalRel();
                rel.setOrderUuid(order.getUuid());
                rel.setFinishUuid(finish.getUuid());
                rel.setOriginalUuid(source.getOriginalUuid());
                rel.setShareRatio(source.getShareRatio());
                rel.setShareWeight(spec.getEstimateWeight() == null ? null : spec.getEstimateWeight()
                        .multiply(source.getShareRatio())
                        .divide(new BigDecimal("100.00"), 3, RoundingMode.HALF_UP));
                rel.setRemark(rewindRemark(dto));
                finishOriginalRelMapper.insert(rel);
            }
            return;
        }
        FinishOriginalRel rel = new FinishOriginalRel();
        rel.setOrderUuid(order.getUuid());
        rel.setFinishUuid(finish.getUuid());
        rel.setOriginalUuid(roll.getUuid());
        rel.setShareRatio(new BigDecimal("100.00"));
        rel.setShareWeight(spec.getEstimateWeight());
        rel.setRemark(dto.getRewindMode() == null ? stepName(roll.getMainStepType()) : rewindRemark(dto));
        finishOriginalRelMapper.insert(rel);
    }

    private List<FinishConfigSpecDTO> buildRewindSaveSpecs(String orderUuid, OriginalRoll roll, FinishConfigSaveDTO dto) {
        if (isOnSite(roll)) {
            return buildOnSiteSaveSpecs(dto.getFinishSpecs());
        }
        if (dto.getRewindSegments() != null && !dto.getRewindSegments().isEmpty()) {
            RewindPlanPreviewDTO previewDto = new RewindPlanPreviewDTO();
            previewDto.setRewindMode(dto.getRewindMode());
            previewDto.setSpareCount(dto.getSpareCount());
            previewDto.setSegments(dto.getRewindSegments());
            FinishPreviewVO preview = buildRewindPreview(orderUuid, roll, previewDto);
            List<FinishPreviewVO.FinishItemPreview> previewFinishes = preview.getFinishes() == null ? List.of() : preview.getFinishes();
            List<FinishPreviewVO.SegmentPreview> previewSegments = preview.getSegments() == null ? List.of() : preview.getSegments();
            List<FinishConfigSpecDTO> specs = new ArrayList<>(previewFinishes.size() + previewSegments.size());
            for (FinishPreviewVO.FinishItemPreview finish : previewFinishes) {
                FinishConfigSpecDTO spec = new FinishConfigSpecDTO();
                spec.setItemType(LAYOUT_ITEM_FINISH);
                spec.setCount(1);
                spec.setFinishWidth(finish.getFinishWidth());
                spec.setFinishDiameter(finish.getFinishDiameter());
                spec.setFinishCoreDiameter(finish.getFinishCoreDiameter());
                spec.setCustomerPaperName(finish.getCustomerPaperName());
                spec.setCustomerGramWeight(finish.getCustomerGramWeight());
                spec.setCustomerFinishWidth(finish.getCustomerFinishWidth());
                spec.setCustomerSpecOverrideReason(finish.getCustomerSpecOverrideReason());
                spec.setEstimateWeight(finish.getEstimateWeight());
                spec.setLayers(finish.getLayers());
                if (dto.getRewindMode() != null && dto.getRewindMode() == 5) {
                    spec.setSources(resolveSegmentSources(dto.getRewindSegments(), finish.getSegmentSort()));
                }
                specs.add(spec);
            }
            specs.addAll(buildRewindTrimSaveSpecs(preview, dto));
            return specs;
        }
        return applyRewindEstimateWeights(orderUuid, roll, dto);
    }

    private List<FinishConfigSpecDTO> buildRewindTrimSaveSpecs(FinishPreviewVO preview, FinishConfigSaveDTO dto) {
        if (preview.getSegments() == null || preview.getSegments().isEmpty()) {
            return List.of();
        }
        List<FinishConfigSpecDTO> specs = new ArrayList<>();
        for (FinishPreviewVO.SegmentPreview segment : preview.getSegments()) {
            if (segment.getTrimWidth() == null || segment.getTrimWidth() <= 0) {
                continue;
            }
            FinishConfigSpecDTO spec = new FinishConfigSpecDTO();
            spec.setItemType(LAYOUT_ITEM_TRIM);
            spec.setCount(1);
            spec.setFinishWidth(segment.getTrimWidth());
            spec.setEstimateWeight(segment.getTrimWeight() == null
                    ? BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP)
                    : segment.getTrimWeight().setScale(3, RoundingMode.HALF_UP));
            if (dto.getRewindMode() != null && dto.getRewindMode() == 5) {
                spec.setSources(resolveSegmentSources(dto.getRewindSegments(), segment.getSegmentSort()));
            }
            specs.add(spec);
        }
        return specs;
    }

    private List<FinishConfigSpecDTO.FinishSourceDTO> resolveSegmentSources(
            List<RewindPlanPreviewDTO.RewindSegmentDTO> segments, Integer segmentSort) {
        if (segments == null || segmentSort == null) {
            return List.of();
        }
        int fallbackSort = 1;
        for (RewindPlanPreviewDTO.RewindSegmentDTO segment : segments) {
            int currentSort = segment.getSegmentSort() == null ? fallbackSort : segment.getSegmentSort();
            if (currentSort == segmentSort) {
                return segment.getSources() == null ? List.of() : segment.getSources();
            }
            fallbackSort++;
        }
        return List.of();
    }

    private List<FinishConfigSpecDTO> applyRewindEstimateWeights(String orderUuid, OriginalRoll roll, FinishConfigSaveDTO dto) {
        List<FinishConfigSpecDTO> specs = dto.getFinishSpecs();
        if (specs == null || specs.isEmpty()) {
            return List.of();
        }
        List<FinishConfigSpecDTO> finishSpecs = specs.stream()
                .filter(spec -> !isTrimSpec(spec))
                .toList();
        List<FinishConfigSpecDTO> trimSpecs = specs.stream()
                .filter(this::isTrimSpec)
                .toList();
        if (dto.getRewindMode() != null && dto.getRewindMode() == 2 && !trimSpecs.isEmpty()) {
            throw new BusinessException("改直径不变门幅模式不能配置修边");
        }
        BigDecimal totalWeight = dto.getRewindMode() != null && dto.getRewindMode() == 5
                ? calcSourceTotalWeight(orderUuid, finishSpecs)
                : calcTotalWeight(roll.getRollWeight(), roll.getPieceNum());
        BigDecimal trimWidth = BigDecimal.ZERO;
        if (roll.getOriginalWidth() != null && roll.getOriginalWidth() > 0 && dto.getRewindMode() != null && dto.getRewindMode() != 2 && dto.getRewindMode() != 5) {
            int explicitTrimWidth = trimSpecs.stream()
                    .mapToInt(spec -> (spec.getFinishWidth() == null ? 0 : spec.getFinishWidth()) * safeCount(spec))
                    .sum();
            int totalFinishWidth = finishSpecs.stream()
                    .mapToInt(spec -> (spec.getFinishWidth() == null ? 0 : spec.getFinishWidth()) * safeCount(spec))
                    .sum();
            if (totalFinishWidth > roll.getOriginalWidth()
                    || (explicitTrimWidth > 0 && totalFinishWidth + explicitTrimWidth > roll.getOriginalWidth())) {
                throw new BusinessException("复卷门幅排布加修边宽度不能超过原纸门幅");
            }
            trimWidth = BigDecimal.valueOf(explicitTrimWidth > 0 ? explicitTrimWidth : Math.max(0, roll.getOriginalWidth() - totalFinishWidth));
        }
        BigDecimal originalWidth = roll.getOriginalWidth() == null ? BigDecimal.ZERO : BigDecimal.valueOf(roll.getOriginalWidth());

        List<RewindWeightCalculator.PieceInput> pieces = new ArrayList<>();
        for (FinishConfigSpecDTO spec : finishSpecs) {
            BigDecimal basis = rewindBasis(roll, dto.getRewindMode(), spec);
            for (int i = 0; i < spec.getCount(); i++) {
                pieces.add(new RewindWeightCalculator.PieceInput(basis, null));
            }
        }

        List<RewindWeightCalculator.PieceResult> results = RewindWeightCalculator.allocate(
                totalWeight,
                pieces,
                trimWidth,
                originalWidth,
                BigDecimal.ZERO);

        int cursor = 0;
        List<FinishConfigSpecDTO> expanded = new ArrayList<>(pieces.size() + (trimWidth.signum() > 0 ? 1 : 0));
        for (FinishConfigSpecDTO spec : finishSpecs) {
            for (int i = 0; i < spec.getCount(); i++) {
                FinishConfigSpecDTO nextSpec = new FinishConfigSpecDTO();
                nextSpec.setItemType(LAYOUT_ITEM_FINISH);
                nextSpec.setFinishWidth(spec.getFinishWidth());
                nextSpec.setFinishDiameter(spec.getFinishDiameter());
                nextSpec.setFinishCoreDiameter(spec.getFinishCoreDiameter());
                nextSpec.setCustomerPaperName(spec.getCustomerPaperName());
                nextSpec.setCustomerGramWeight(spec.getCustomerGramWeight());
                nextSpec.setCustomerFinishWidth(spec.getCustomerFinishWidth());
                nextSpec.setCustomerSpecOverrideReason(spec.getCustomerSpecOverrideReason());
                nextSpec.setCount(1);
                nextSpec.setSplitRatio(spec.getSplitRatio());
                nextSpec.setSources(spec.getSources());
                nextSpec.setLayers(spec.getLayers());
                nextSpec.setEstimateWeight(results.get(cursor++).weight);
                expanded.add(nextSpec);
            }
        }
        if (trimWidth.signum() > 0) {
            expanded.add(rewindTrimSpec(trimWidth, totalWeight, originalWidth, trimSpecs));
        }
        return expanded;
    }

    private FinishConfigSpecDTO rewindTrimSpec(BigDecimal trimWidth, BigDecimal totalWeight, BigDecimal originalWidth,
                                               List<FinishConfigSpecDTO> trimSpecs) {
        FinishConfigSpecDTO spec = new FinishConfigSpecDTO();
        spec.setItemType(LAYOUT_ITEM_TRIM);
        spec.setCount(1);
        spec.setFinishWidth(trimWidth.intValue());
        spec.setEstimateWeight(originalWidth.signum() > 0
                ? totalWeight.multiply(trimWidth).divide(originalWidth, 3, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
        if (!trimSpecs.isEmpty()) {
            spec.setSources(trimSpecs.get(0).getSources());
        }
        return spec;
    }

    private BigDecimal calcSourceTotalWeight(String orderUuid, List<FinishConfigSpecDTO> specs) {
        List<OriginalRoll> orderRolls = originalRollMapper.selectList(new LambdaQueryWrapper<OriginalRoll>()
                .eq(OriginalRoll::getOrderUuid, orderUuid));
        Map<String, OriginalRoll> rollByUuid = new LinkedHashMap<>();
        for (OriginalRoll sourceRoll : orderRolls) {
            rollByUuid.put(sourceRoll.getUuid(), sourceRoll);
        }
        Set<String> sourceUuids = new LinkedHashSet<>();
        for (FinishConfigSpecDTO spec : specs) {
            if (spec.getSources() == null) {
                continue;
            }
            for (FinishConfigSpecDTO.FinishSourceDTO source : spec.getSources()) {
                sourceUuids.add(source.getOriginalUuid());
            }
        }
        BigDecimal total = BigDecimal.ZERO;
        for (String sourceUuid : sourceUuids) {
            OriginalRoll sourceRoll = rollByUuid.get(sourceUuid);
            if (sourceRoll != null) {
                total = total.add(calcTotalWeight(sourceRoll.getRollWeight(), sourceRoll.getPieceNum()));
            }
        }
        return total;
    }

    private BigDecimal rewindBasis(OriginalRoll roll, Integer rewindMode, FinishConfigSpecDTO spec) {
        if (rewindMode == null) {
            return BigDecimal.ZERO;
        }
        if (rewindMode == 1 || rewindMode == 6) {
            return BigDecimal.valueOf(spec.getFinishWidth() == null ? 0 : spec.getFinishWidth());
        }
        if (rewindMode == 2) {
            return calcLayerArea(spec.getFinishDiameter(), spec.getFinishCoreDiameter());
        }
        if (rewindMode == 3) {
            BigDecimal originalWidth = roll.getOriginalWidth() == null ? BigDecimal.ZERO : BigDecimal.valueOf(roll.getOriginalWidth());
            if (originalWidth.signum() == 0) {
                return calcLayerArea(spec.getFinishDiameter(), spec.getFinishCoreDiameter());
            }
            BigDecimal width = BigDecimal.valueOf(spec.getFinishWidth() == null ? 0 : spec.getFinishWidth());
            return calcLayerArea(spec.getFinishDiameter(), spec.getFinishCoreDiameter())
                    .multiply(width.divide(originalWidth, 6, RoundingMode.HALF_UP));
        }
        if (rewindMode == 4) {
            BigDecimal sum = BigDecimal.ZERO;
            if (spec.getLayers() != null) {
                for (FinishConfigSpecDTO.FinishLayerDTO layer : spec.getLayers()) {
                    sum = sum.add(calcLayerArea(layer.getOutDiameter(), layer.getCoreDiameter()));
                }
            }
            return sum;
        }
        return spec.getSplitRatio() == null ? BigDecimal.ZERO : spec.getSplitRatio();
    }

    private BigDecimal calcSpecArea(Integer rewindMode, FinishConfigSpecDTO spec, OriginalRoll roll) {
        if (rewindMode == null) {
            return BigDecimal.ZERO;
        }
        if (rewindMode == 4) {
            return rewindBasis(roll, rewindMode, spec);
        }
        return rewindBasis(roll, rewindMode, spec);
    }

    private BigDecimal calcLayerArea(Integer outDiameter, Integer coreDiameter) {
        if (outDiameter == null || coreDiameter == null) {
            return BigDecimal.ZERO;
        }
        return RewindWeightCalculator.crossSectionArea(
                RewindWeightCalculator.inchToMm(BigDecimal.valueOf(outDiameter)),
                RewindWeightCalculator.inchToMm(BigDecimal.valueOf(coreDiameter)));
    }

    private BigDecimal layoutLayerArea(List<FinishConfigSpecDTO.FinishLayerDTO> layers) {
        BigDecimal sum = BigDecimal.ZERO;
        for (FinishConfigSpecDTO.FinishLayerDTO layer : layers) {
            sum = sum.add(calcLayerArea(layer.getOutDiameter(), layer.getCoreDiameter()));
        }
        return sum;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException("复卷参数序列化失败");
        }
    }

    private FinishRoll buildFinishRoll(ProcessOrder order, OriginalRoll roll, FinishConfigSpecDTO spec,
                                       int rowSort, int isSpare) {
        FinishRoll finish = new FinishRoll();
        boolean trimSpec = spec != null && isTrimSpec(spec);
        finish.setOrderUuid(order.getUuid());
        finish.setRowSort(rowSort);
        finish.setRollNoStatus(ROLL_NO_PRE);
        finish.setIsSpare(isSpare);
        finish.setIsRemain(trimSpec ? IS_REMAIN_YES : IS_REMAIN_NO);
        finish.setPaperName(roll.getPaperName() == null ? "待定" : roll.getPaperName());
        finish.setGramWeight(roll.getGramWeight() == null ? 0 : roll.getGramWeight());
        finish.setSourceType(1);
        finish.setFinishStatus(1);
        finish.setWarehouseUuid(order.getWarehouseUuid());
        finish.setOriginalRollNos(finishOriginalKey(roll));
        if (spec == null) {
            finish.setFinishWidth(0);
            return finish;
        }
        finish.setFinishWidth(spec.getFinishWidth() == null ? 0 : spec.getFinishWidth());
        finish.setFinishDiameter(spec.getFinishDiameter());
        finish.setFinishCoreDiameter(spec.getFinishCoreDiameter());
        finish.setEstimateWeight(spec.getEstimateWeight());
        finish.setEstimateWeightSnap(spec.getEstimateWeight());
        if (!trimSpec) {
            FinishCustomerSpecificationPolicy.apply(finish, spec, currentOperator());
        }
        if (trimSpec) {
            finish.setRemark("修边/余料");
        }
        return finish;
    }

    private String allocAndInsertFinish(FinishRoll finish) {
        for (int attempt = 0; attempt < 5; attempt++) {
            finish.setUuid(null);
            finish.setFinishRollNo(nextFinishRollNo());
            try {
                finishRollMapper.insert(finish);
                return finish.getFinishRollNo();
            } catch (DuplicateKeyException e) {
                // 并发抢号后重算。
            }
        }
        throw new BusinessException("卷号分配冲突，请重试");
    }

    private List<String> createServiceOnlyFinishes(ProcessOrder order, OriginalRoll roll) {
        int count = roll.getPieceNum() == null ? 1 : roll.getPieceNum();
        int rowSort = nextFinishRowSort(order.getUuid());
        List<String> rollNos = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            FinishRoll finish = ServiceOnlyFinishFactory.create(order, roll, rowSort++);
            rollNos.add(allocAndInsertFinish(finish));
            finishRollSourceBinder.bind(new FinishRollSourceBinder.BindRequest(
                    order.getUuid(), finish, roll.getUuid(), "仅附加工艺来源"));
        }
        return rollNos;
    }

    private String nextFinishRollNo() {
        return rollNoSequenceService.nextFinishRollNo();
    }

    private int nextFinishRowSort(String orderUuid) {
        FinishRoll top = finishRollMapper.selectOne(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, orderUuid)
                .orderByDesc(FinishRoll::getRowSort)
                .last("LIMIT 1"));
        if (top == null || top.getRowSort() == null) {
            return 1;
        }
        return top.getRowSort() + 1;
    }

    private String finishOriginalKey(OriginalRoll roll) {
        return StringUtils.hasText(roll.getRollNo()) ? roll.getRollNo() : roll.getUuid();
    }

    private static String stepName(Integer stepType) {
        if (stepType != null && stepType == FeeCalculator.STEP_TYPE_SAW) {
            return "锯纸";
        }
        if (stepType != null && stepType == FeeCalculator.STEP_TYPE_REWIND) {
            return "复卷";
        }
        if (stepType != null && stepType == FeeCalculator.STEP_TYPE_STRIP_SORT) {
            return "剥损整理";
        }
        if (stepType != null && stepType == FeeCalculator.STEP_TYPE_REPACKAGE) {
            return "重新包装";
        }
        return null;
    }

    private boolean isDirectShip(OriginalRoll roll) {
        return roll.getProcessMode() != null && roll.getProcessMode() == PROCESS_MODE_DIRECT_SHIP;
    }

    private boolean isServiceOnly(OriginalRoll roll) {
        return ProcessModePolicy.isServiceOnly(roll.getProcessMode());
    }

    private boolean requiresMainProcess(OriginalRoll roll) {
        return ProcessModePolicy.requiresMainProcess(roll.getProcessMode());
    }

    private boolean isOnSite(OriginalRoll roll) {
        return roll.getProcessMode() != null && roll.getProcessMode() == PROCESS_MODE_ON_SITE;
    }

    private BigDecimal calcTotalWeight(BigDecimal rollWeight, Integer pieceNum) {
        return rollWeight.multiply(BigDecimal.valueOf(pieceNum));
    }

    /** 取该加工单当前有效明细的最大 row_sort + 1。 */
    private int nextRowSort(String orderUuid) {
        List<OriginalRoll> rolls = originalRollMapper.selectList(
                new LambdaQueryWrapper<OriginalRoll>()
                        .eq(OriginalRoll::getOrderUuid, orderUuid)
                        .orderByDesc(OriginalRoll::getRowSort)
                        .last("LIMIT 1"));
        if (rolls.isEmpty() || rolls.get(0).getRowSort() == null) {
            return 1;
        }
        return rolls.get(0).getRowSort() + 1;
    }

    /** 生成加工单号：由系统单号规则配置生成，唯一索引 uk_order_no 兜底防并发重复。 */
    private String nextOrderNo(LocalDate orderDate) {
        return documentNoService.next(NoRuleBizType.PROCESS_ORDER, orderDate);
    }

    // ==================== Phase 5.1：追加工序功能 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addProcessStep(String orderUuid, ProcessStepDTO dto) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = requireOrder(orderUuid);
        validateAddStepStatus(order, dto);
        validateStepRollBelongsToOrder(orderUuid, dto.getOriginalUuid());
        validateMainStepUnique(dto);

        // 3. 自动分配stepSort
        Integer maxSort = processStepMapper.selectMaxStepOrder(dto.getOriginalUuid());
        int stepSort = maxSort == null ? 1 : maxSort + 1;

        // 4. 构建工序实体
        OriginalRoll roll = originalRollMapper.selectById(dto.getOriginalUuid());
        ProcessStep step = buildProcessStep(orderUuid, dto, stepSort, roll);

        // 5. 插入工序
        processStepMapper.insert(step);

        // 6. 自动更新is_mix_process标识
        updateMixProcessFlag(orderUuid);

        // 7. 重算计费
        calcFee(orderUuid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessStepBatchResultVO addProcessSteps(String orderUuid, ProcessStepBatchDTO dto) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = requireOrder(orderUuid);
        List<ProcessStepDTO> requests = dto.getSteps();
        requests.forEach(request -> validateBatchServiceStep(order, request));
        Map<String, OriginalRoll> rolls = loadBatchOriginalRolls(orderUuid, requests);
        ProcessStepBatchResultVO result = serviceStepBatchUpsertWriter.upsert(orderUuid, requests, rolls);
        updateMixProcessFlag(orderUuid);
        calcFee(orderUuid);
        return result;
    }

    private void validateBatchServiceStep(ProcessOrder order, ProcessStepDTO dto) {
        validateAddStepStatus(order, dto);
        if (!isServiceStep(dto.getStepType()) || Integer.valueOf(STEP_MAIN).equals(dto.getIsMain())) {
            throw new BusinessException("批量应用只支持剥损整理或重新包装");
        }
    }

    private Map<String, OriginalRoll> loadBatchOriginalRolls(String orderUuid, List<ProcessStepDTO> requests) {
        Set<String> originalUuids = requests.stream().map(ProcessStepDTO::getOriginalUuid)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<String, OriginalRoll> rolls = originalRollMapper.selectList(new LambdaQueryWrapper<OriginalRoll>()
                        .eq(OriginalRoll::getOrderUuid, orderUuid)
                        .in(OriginalRoll::getUuid, originalUuids))
                .stream().collect(java.util.stream.Collectors.toMap(OriginalRoll::getUuid, roll -> roll));
        if (rolls.size() != originalUuids.size()) {
            throw new BusinessException(ErrorCode.E002, "部分原纸明细不存在或不属于当前加工单");
        }
        if (rolls.values().stream().anyMatch(roll -> !ProcessModePolicy.supportsServiceSteps(roll.getProcessMode()))) {
            throw new BusinessException("批量附加工艺只能应用到锯纸、复卷或仅附加工艺母卷");
        }
        return rolls;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProcessStep(String stepUuid, ProcessStepDTO dto) {
        // 1. 查询工序
        ProcessStep step = processStepMapper.selectById(stepUuid);
        if (step == null) {
            throw new BusinessException(ErrorCode.E002, "工序不存在");
        }
        businessLockService.lockProcessOrders(List.of(step.getOrderUuid()));
        step = processStepMapper.selectById(stepUuid);
        if (step == null) {
            throw new BusinessException(ErrorCode.E006, "工序已被其他操作删除，请刷新后重试");
        }

        // 2. 校验状态
        ProcessOrder order = getById(step.getOrderUuid());
        ProcessStepEditStatusPolicy.requireChangeAllowed(order.getOrderStatus());

        // 3. 更新工序（不允许修改orderUuid、originalUuid、isMain）
        if (dto.getStepType() != null && !dto.getStepType().equals(step.getStepType())) {
            step.setStepType(dto.getStepType());
            step.setBillingBasis(null);
            step.setServiceQuantity(null);
        }
        if (dto.getStepName() != null) step.setStepName(dto.getStepName());
        if (!StringUtils.hasText(step.getStepName())) step.setStepName(stepName(step.getStepType()));
        if (dto.getKnifeCount() != null) step.setKnifeCount(dto.getKnifeCount());
        if (dto.getProcessWeight() != null) step.setProcessWeight(dto.getProcessWeight());
        if (dto.getBillingBasis() != null) step.setBillingBasis(dto.getBillingBasis().trim().toUpperCase());
        if (dto.getServiceQuantity() != null) step.setServiceQuantity(dto.getServiceQuantity());
        if (dto.getBillingMode() != null) step.setBillingMode(dto.getBillingMode());
        if (dto.getBillingAmount() != null) step.setBillingAmount(dto.getBillingAmount());
        if (dto.getUnitPrice() != null) step.setUnitPrice(dto.getUnitPrice());
        if (isServiceStep(step.getStepType()) || dto.getMachineUuid() != null) {
            applyMachine(step, dto.getMachineUuid(), null);
        }
        if (dto.getRemark() != null) step.setRemark(dto.getRemark());
        applyServicePricingFields(step, dto);
        OriginalRoll roll = originalRollMapper.selectById(step.getOriginalUuid());
        applyServiceQuantity(step, roll);
        validateServiceStep(step, roll);

        ConcurrencyGuard.requireRowUpdated(processStepMapper.updateById(step));

        // 4. 自动更新is_mix_process标识
        updateMixProcessFlag(step.getOrderUuid());

        // 5. 重算计费
        calcFee(step.getOrderUuid());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProcessStep(String stepUuid) {
        ProcessStep step = processStepMapper.selectById(stepUuid);
        if (step == null) {
            throw new BusinessException(ErrorCode.E002, "工序不存在");
        }
        businessLockService.lockProcessOrders(List.of(step.getOrderUuid()));
        step = processStepMapper.selectById(stepUuid);
        if (step == null) {
            throw new BusinessException(ErrorCode.E006, "工序已被其他操作删除，请刷新后重试");
        }

        // 2. 校验状态
        ProcessOrder order = getById(step.getOrderUuid());
        ProcessStepEditStatusPolicy.requireChangeAllowed(order.getOrderStatus());

        // 3. 主工艺不可删除
        if (step.getIsMain() != null && step.getIsMain() == 1) {
            throw new BusinessException(ErrorCode.E003, "主工艺不可删除");
        }

        // 4. 软删除
        ConcurrencyGuard.requireRowUpdated(processStepMapper.deleteById(stepUuid));

        // 5. 自动更新is_mix_process标识
        updateMixProcessFlag(step.getOrderUuid());

        // 6. 重算计费
        calcFee(step.getOrderUuid());
    }

    private void validateAddStepStatus(ProcessOrder order, ProcessStepDTO dto) {
        boolean extraStep = dto.getIsMain() == null || dto.getIsMain() == 0;
        ProcessStepEditStatusPolicy.requireAddAllowed(order.getOrderStatus(), extraStep);
    }

    private void validateStepRollBelongsToOrder(String orderUuid, String originalUuid) {
        OriginalRoll roll = originalRollMapper.selectById(originalUuid);
        if (roll == null || !orderUuid.equals(roll.getOrderUuid())) {
            throw new BusinessException(ErrorCode.E002, "原纸明细不存在");
        }
    }

    private void validateMainStepUnique(ProcessStepDTO dto) {
        if (dto.getIsMain() == null || dto.getIsMain() != 1) {
            return;
        }
        long count = processStepMapper.selectCount(
                new LambdaQueryWrapper<ProcessStep>()
                        .eq(ProcessStep::getOriginalUuid, dto.getOriginalUuid())
                        .eq(ProcessStep::getIsMain, 1)
                        .eq(ProcessStep::getIsDeleted, 0)
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.E003, "该母卷已存在主工艺，不可重复添加");
        }
    }

    private ProcessStep buildProcessStep(String orderUuid, ProcessStepDTO dto, int stepSort, OriginalRoll roll) {
        ProcessStep step = new ProcessStep();
        BeanUtils.copyProperties(dto, step);
        step.setOrderUuid(orderUuid);
        step.setStepSort(stepSort);
        if (!StringUtils.hasText(step.getStepName())) step.setStepName(stepName(step.getStepType()));
        applyMachine(step, dto.getMachineUuid(), roll == null ? null : roll.getMachineUuid());
        if (step.getIsMain() == null) {
            step.setIsMain(0);
        }
        if (StringUtils.hasText(step.getBillingBasis())) {
            step.setBillingBasis(step.getBillingBasis().trim().toUpperCase());
        }
        applyServiceQuantity(step, roll);
        validateServiceStep(step, roll);
        return step;
    }

    private void validateServiceStep(ProcessStep step, OriginalRoll roll) {
        var catalog = processCatalogStepValidator.validate(step, roll);
        if (!StringUtils.hasText(step.getStepName())) {
            step.setStepName(catalog.name());
        }
        if (!"SERVICE_QUANTITY".equals(catalog.pricingStrategy())) {
            return;
        }
        if (Integer.valueOf(STEP_MAIN).equals(step.getIsMain())) {
            throw new BusinessException("整理和包装只能作为服务工序");
        }
        int mode = step.getBillingMode() == null ? ProcessStepPricingPolicy.STANDARD : step.getBillingMode();
        if (mode == ProcessStepPricingPolicy.QUANTITY_OVERRIDE) {
            throw new BusinessException("整理和包装不使用指定数量模式，请直接填写服务数量");
        }
        if (mode == ProcessStepPricingPolicy.FIXED_AMOUNT) {
            if (step.getBillingAmount() == null || step.getBillingAmount().signum() < 0) {
                throw new BusinessException("固定金额服务必须填写不小于0的金额");
            }
            return;
        }
        if (mode == ProcessStepPricingPolicy.FREE) {
            return;
        }
        if (step.getServiceQuantity() == null || step.getServiceQuantity().signum() <= 0) {
            throw new BusinessException("按件或按吨服务必须填写服务数量");
        }
        if (!StringUtils.hasText(step.getBillingBasis())) {
            throw new BusinessException("按件或按吨服务必须选择计费基准");
        }
        if (!"TON".equals(step.getBillingBasis()) && !"PIECE".equals(step.getBillingBasis())) {
            throw new BusinessException("服务计费基准只能选择按件或按吨");
        }
        if ("PIECE".equals(step.getBillingBasis())
                && step.getServiceQuantity().stripTrailingZeros().scale() > 0) {
            throw new BusinessException("按件服务数量必须为整数");
        }
        if (step.getUnitPrice() != null && step.getUnitPrice().signum() <= 0) {
            throw new BusinessException("服务单价必须大于0，暂不定价请留空");
        }
    }

    private void applyServiceQuantity(ProcessStep step, OriginalRoll roll) {
        if (!isServiceStep(step.getStepType())) return;
        int mode = step.getBillingMode() == null ? ProcessStepPricingPolicy.STANDARD : step.getBillingMode();
        if (mode == ProcessStepPricingPolicy.FIXED_AMOUNT || mode == ProcessStepPricingPolicy.FREE) {
            step.setServiceQuantity(null);
            return;
        }
        step.setServiceQuantity(ServiceStepQuantityResolver.resolve(step.getBillingBasis(), roll));
    }

    private void applyServicePricingFields(ProcessStep step, ProcessStepDTO dto) {
        if (!isServiceStep(step.getStepType())) return;
        step.setBillingMode(dto.getBillingMode() == null ? ProcessStepPricingPolicy.STANDARD : dto.getBillingMode());
        step.setBillingBasis(StringUtils.hasText(dto.getBillingBasis())
                ? dto.getBillingBasis().trim().toUpperCase() : null);
        step.setUnitPrice(dto.getUnitPrice());
        step.setBillingAmount(dto.getBillingAmount());
    }

    private boolean isServiceStep(Integer stepType) {
        return Integer.valueOf(FeeCalculator.STEP_TYPE_STRIP_SORT).equals(stepType)
                || Integer.valueOf(FeeCalculator.STEP_TYPE_REPACKAGE).equals(stepType);
    }

    private void applyMachine(ProcessStep step, String requestedMachineUuid, String fallbackMachineUuid) {
        String fallback = isServiceStep(step.getStepType()) ? null : fallbackMachineUuid;
        String machineUuid = StringUtils.hasText(requestedMachineUuid) ? requestedMachineUuid : fallback;
        step.setMachineUuid(machineUuid);
        step.setMachineNameSnap(resolveMachineName(machineUuid));
    }

    private String resolveMachineName(String machineUuid) {
        if (!StringUtils.hasText(machineUuid)) {
            return null;
        }
        Machine machine = machineMapper.selectById(machineUuid);
        return machine == null ? null : machine.getMachineName();
    }

    private ProcessOrder requireOrder(String uuid) {
        ProcessOrder order = getById(uuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        return order;
    }

    private LockedRoll lockRollAndOrder(String rollUuid) {
        OriginalRoll initial = originalRollMapper.selectById(rollUuid);
        if (initial == null) {
            throw new BusinessException(ErrorCode.E002, "原纸明细不存在");
        }
        businessLockService.lockProcessOrders(List.of(initial.getOrderUuid()));
        OriginalRoll roll = originalRollMapper.selectById(rollUuid);
        if (roll == null) {
            throw new BusinessException(ErrorCode.E006, "母卷已被其他操作删除，请刷新后重试");
        }
        return new LockedRoll(roll, requireOrder(roll.getOrderUuid()));
    }

    private void validateRollStructureEditable(ProcessOrder order) {
        if (!isDraftOrPending(order)) {
            throw new BusinessException(ErrorCode.E004, "母卷规格和明细只能在草稿或待下发阶段修改");
        }
    }

    private boolean hasStructuralRollChange(OriginalRoll before, OriginalRoll after) {
        return !Objects.equals(before.getRollNo(), after.getRollNo())
                || !Objects.equals(before.getPaperName(), after.getPaperName())
                || !Objects.equals(before.getGramWeight(), after.getGramWeight())
                || !Objects.equals(before.getOriginalWidth(), after.getOriginalWidth())
                || !Objects.equals(before.getOriginalDiameter(), after.getOriginalDiameter())
                || !Objects.equals(before.getCoreDiameter(), after.getCoreDiameter())
                || !Objects.equals(before.getOriginalLength(), after.getOriginalLength())
                || !Objects.equals(before.getRollWeight(), after.getRollWeight())
                || !Objects.equals(before.getPieceNum(), after.getPieceNum())
                || !Objects.equals(before.getProcessMode(), after.getProcessMode())
                || !Objects.equals(before.getMainStepType(), after.getMainStepType())
                || !Objects.equals(before.getMachineUuid(), after.getMachineUuid());
    }

    private void validateRollProductionEditable(ProcessOrder order) {
        Integer status = order.getOrderStatus();
        if (status == null || status < STATUS_DRAFT || status > STATUS_TO_RECORD) {
            throw new BusinessException(ErrorCode.E004, "加工单已完成或锁定，不能再修改母卷生产记录");
        }
    }

    private void validateRemarkEditable(ProcessOrder order) {
        Integer status = order.getOrderStatus();
        if (status != null && (status == STATUS_SETTLED || status == STATUS_VOIDED)) {
            throw new BusinessException(ErrorCode.E003, "当前状态不允许直接修改备注");
        }
    }

    private record LockedRoll(OriginalRoll roll, ProcessOrder order) {
    }

    private void recordFieldIfChanged(String orderUuid, String orderNo, String fieldName,
                                      String oldValue, String newValue, String operator) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        operationLogService.recordField(BIZ_TYPE_ORDER, orderUuid, orderNo, fieldName, oldValue, newValue, operator);
    }

    private String rollLabel(OriginalRoll roll) {
        if (StringUtils.hasText(roll.getRollNo())) {
            return "原纸" + roll.getRollNo();
        }
        if (StringUtils.hasText(roll.getExtraNo())) {
            return "原纸" + roll.getExtraNo();
        }
        return "原纸" + (roll.getRowSort() == null ? roll.getUuid() : roll.getRowSort());
    }

    private String currentOperator() {
        return AuthContextHolder.currentDisplayName();
    }

    /**
     * 更新混合工艺标识：检查该加工单下是否存在不同工序类型（锯纸/复卷），
     * 或同一原纸卷存在多个工序。
     */
    private void updateMixProcessFlag(String orderUuid) {
        List<ProcessStep> steps = processStepMapper.selectList(
                new LambdaQueryWrapper<ProcessStep>()
                        .eq(ProcessStep::getOrderUuid, orderUuid)
        );

        // 更新标识
        ProcessOrder orderToUpdate = getById(orderUuid);
        orderToUpdate.setIsMixProcess(ProcessMixProcessResolver.isMix(steps) ? 1 : 0);
        updateById(orderToUpdate);
    }
}
