package com.paper.mes.processorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.dto.DraftOrderVO;
import com.paper.mes.processorder.dto.DraftOrderBaseDTO;
import com.paper.mes.processorder.dto.DraftSummaryVO;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.dto.OriginalRollImportPreviewVO;
import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanBatchSaveDTO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.ProcessOrderSubmitVO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessConfigDraft;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessConfigDraftMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.service.DraftOrderReader;
import com.paper.mes.processorder.service.OriginalRollImportParser;
import com.paper.mes.processorder.service.ProcessPlanDraftManager;
import com.paper.mes.processorder.service.ProcessPlanMapper;
import com.paper.mes.processorder.service.ProcessOrderDraftService;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.processorder.service.ProcessRouteDraftManager;
import com.paper.mes.system.config.constant.NoRuleBizType;
import com.paper.mes.system.config.service.DocumentNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProcessOrderDraftServiceImpl implements ProcessOrderDraftService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PENDING = 1;
    private static final int PROCESS_MODE_DIRECT = 3;
    private static final int REWIND_MODE_MULTI_SOURCE = 5;
    private static final int ROLL_STATUS_PENDING = 1;
    private static final int ROLL_NO_VOID = 3;
    private static final int IS_SPARE_YES = 1;
    private static final int DEFAULT_SETTLE_TYPE = 2;
    private static final int DEFAULT_IS_INVOICE = 2;
    private final ProcessOrderMapper processOrderMapper;
    private final OriginalRollMapper originalRollMapper;
    private final ProcessConfigDraftMapper draftMapper;
    private final FinishRollMapper finishRollMapper;
    private final CustomerService customerService;
    private final ProcessOrderService processOrderService;
    private final DraftOrderReader draftOrderReader;
    private final ProcessPlanDraftManager planDraftManager;
    private final ProcessRouteDraftManager routeDraftManager;
    private final ProcessPlanMapper processPlanMapper;
    private final OriginalRollImportParser importParser;
    private final ObjectMapper objectMapper;
    private final DocumentNoService documentNoService;
    private final BusinessLockService businessLockService;

    @Override
    public List<DraftSummaryVO> listDrafts() {
        return draftOrderReader.listDrafts();
    }

    @Override
    public DraftOrderVO getDraft(String orderUuid) {
        return draftOrderReader.getDraft(orderUuid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createDraft(DraftOrderBaseDTO dto) {
        Customer customer = requireCustomer(dto.getCustomerUuid());
        ProcessOrder order = new ProcessOrder();
        copyBaseFields(dto, order, customer);
        order.setCustomerName(customer.getCustomerName());
        order.setOrderNo(nextOrderNo(dto));
        order.setOrderStatus(STATUS_DRAFT);
        processOrderMapper.insert(order);
        return order.getUuid();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBaseInfo(String orderUuid, DraftOrderBaseDTO dto) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = requireDraft(orderUuid);
        Customer customer = requireCustomer(dto.getCustomerUuid());
        copyBaseFields(dto, order, customer);
        order.setCustomerName(customer.getCustomerName());
        processOrderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDraftProgress(String orderUuid, Integer currentStep) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = requireDraft(orderUuid);
        order.setExtNum1(currentStep == null ? BigDecimal.ZERO : BigDecimal.valueOf(currentStep));
        processOrderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> replaceOriginalRolls(String orderUuid, List<OriginalRollDTO> rolls) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = requireDraft(orderUuid);
        deleteDraftRolls(orderUuid);
        List<String> rollUuids = new ArrayList<>(rolls.size());
        for (int i = 0; i < rolls.size(); i++) {
            OriginalRoll roll = buildDraftRoll(order, rolls.get(i), i + 1);
            originalRollMapper.insert(roll);
            rollUuids.add(roll.getUuid());
        }
        return rollUuids;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveProcessConfig(String orderUuid, String rollUuid, FinishConfigSaveDTO dto) {
        saveProcessPlan(orderUuid, rollUuid, processPlanMapper.fromSaveDto(dto));
    }

    @Override
    public PlanPreviewVO previewProcessPlan(String orderUuid, String rollUuid, ProcessPlanDTO plan) {
        return planDraftManager.previewProcessPlan(orderUuid, rollUuid, plan);
    }

    @Override
    public PlanPreviewVO saveProcessPlan(String orderUuid, String rollUuid, ProcessPlanDTO plan) {
        return planDraftManager.saveProcessPlan(orderUuid, rollUuid, plan);
    }

    @Override
    public List<PlanPreviewVO> saveProcessPlanBatch(String orderUuid, ProcessPlanBatchSaveDTO dto) {
        return planDraftManager.saveBatch(orderUuid, dto);
    }

    @Override
    public OriginalRollImportPreviewVO importPreview(MultipartFile file) {
        return importParser.parse(file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessOrderSubmitVO submit(String orderUuid) {
        businessLockService.lockProcessOrders(List.of(orderUuid));
        ProcessOrder order = requireOrder(orderUuid);
        if (STATUS_PENDING == order.getOrderStatus()) {
            return submittedResult(order);
        }
        if (order.getOrderStatus() == null || order.getOrderStatus() != STATUS_DRAFT) {
            throw new BusinessException(ErrorCode.E001, "只有草稿加工单可提交");
        }
        List<OriginalRoll> rolls = listRolls(orderUuid);
        Map<String, ProcessConfigDraft> drafts = draftMap(orderUuid);
        validateSubmit(rolls, drafts);
        order.setOrderStatus(STATUS_PENDING);
        ConcurrencyGuard.requireRowUpdated(processOrderMapper.updateById(order));
        return generateFinishConfigs(order, rolls, drafts);
    }

    private void copyBaseFields(DraftOrderBaseDTO dto, ProcessOrder order, Customer customer) {
        BeanUtils.copyProperties(dto, order);
        applyCustomerDefaults(dto, order, customer);
    }

    private void applyCustomerDefaults(DraftOrderBaseDTO dto, ProcessOrder order, Customer customer) {
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

    private Customer requireCustomer(String customerUuid) {
        Customer customer = customerService.getById(customerUuid);
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }
        return customer;
    }

    private ProcessOrder requireOrder(String orderUuid) {
        ProcessOrder order = processOrderMapper.selectById(orderUuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "加工单不存在");
        }
        return order;
    }

    private ProcessOrder requireDraft(String orderUuid) {
        ProcessOrder order = requireOrder(orderUuid);
        if (order.getOrderStatus() == null || order.getOrderStatus() != STATUS_DRAFT) {
            throw new BusinessException(ErrorCode.E001, "只有草稿加工单可编辑");
        }
        return order;
    }

    private OriginalRoll requireRoll(String orderUuid, String rollUuid) {
        OriginalRoll roll = originalRollMapper.selectById(rollUuid);
        if (roll == null || !orderUuid.equals(roll.getOrderUuid())) {
            throw new BusinessException(ErrorCode.E002, "原纸明细不存在");
        }
        return roll;
    }

    private OriginalRoll buildDraftRoll(ProcessOrder order, OriginalRollDTO dto, int rowSort) {
        OriginalRoll roll = new OriginalRoll();
        BeanUtils.copyProperties(dto, roll);
        roll.setOrderUuid(order.getUuid());
        roll.setOrderNo(order.getOrderNo());
        roll.setCustomerName(order.getCustomerName());
        roll.setRowSort(rowSort);
        roll.setRollStatus(ROLL_STATUS_PENDING);
        roll.setPieceNum(dto.getPieceNum() == null ? 1 : dto.getPieceNum());
        roll.setTotalWeight(totalWeight(roll.getRollWeight(), roll.getPieceNum()));
        return roll;
    }

    private BigDecimal totalWeight(BigDecimal weight, Integer pieces) {
        return weight.multiply(BigDecimal.valueOf(pieces == null ? 1 : pieces));
    }

    private void deleteDraftRolls(String orderUuid) {
        draftMapper.delete(new LambdaQueryWrapper<ProcessConfigDraft>()
                .eq(ProcessConfigDraft::getOrderUuid, orderUuid));
        originalRollMapper.delete(new LambdaQueryWrapper<OriginalRoll>()
                .eq(OriginalRoll::getOrderUuid, orderUuid));
    }

    private ProcessConfigDraft selectDraft(String orderUuid, String rollUuid) {
        return draftMapper.selectOne(new LambdaQueryWrapper<ProcessConfigDraft>()
                .eq(ProcessConfigDraft::getOrderUuid, orderUuid)
                .eq(ProcessConfigDraft::getOriginalUuid, rollUuid)
                .last("LIMIT 1"));
    }

    private List<OriginalRoll> listRolls(String orderUuid) {
        return originalRollMapper.selectList(new LambdaQueryWrapper<OriginalRoll>()
                .eq(OriginalRoll::getOrderUuid, orderUuid)
                .orderByAsc(OriginalRoll::getRowSort));
    }

    private Map<String, ProcessConfigDraft> draftMap(String orderUuid) {
        Map<String, ProcessConfigDraft> map = new LinkedHashMap<>();
        List<ProcessConfigDraft> drafts = draftMapper.selectList(new LambdaQueryWrapper<ProcessConfigDraft>()
                .eq(ProcessConfigDraft::getOrderUuid, orderUuid));
        drafts.forEach(draft -> map.put(draft.getOriginalUuid(), draft));
        return map;
    }

    private void validateSubmit(List<OriginalRoll> rolls, Map<String, ProcessConfigDraft> drafts) {
        if (rolls.isEmpty()) {
            throw new BusinessException("加工单至少需要录入一条原纸");
        }
        Set<String> coveredRolls = coveredByMultiSourceDrafts(drafts);
        for (OriginalRoll roll : rolls) {
            if (roll.getProcessMode() != null && roll.getProcessMode() == PROCESS_MODE_DIRECT) {
                continue;
            }
            if (coveredRolls.contains(roll.getUuid())) {
                continue;
            }
            ProcessConfigDraft draft = drafts.get(roll.getUuid());
            if (draft == null || draft.getConfigStatus() == null || draft.getConfigStatus() != 1) {
                throw new BusinessException("原纸尚未完成工艺配置：" + (roll.getRollNo() == null ? roll.getPaperName() : roll.getRollNo()));
            }
        }
    }

    private ProcessOrderSubmitVO generateFinishConfigs(ProcessOrder order, List<OriginalRoll> rolls,
                                                       Map<String, ProcessConfigDraft> drafts) {
        Set<String> coveredRolls = coveredByMultiSourceDrafts(drafts);
        for (OriginalRoll roll : rolls) {
            if (roll.getProcessMode() != null && roll.getProcessMode() == PROCESS_MODE_DIRECT) {
                continue;
            }
            if (coveredRolls.contains(roll.getUuid())) {
                continue;
            }
            ProcessConfigDraft draft = drafts.get(roll.getUuid());
            if (routeDraftManager.isRouteDraft(draft)) {
                routeDraftManager.submit(order, roll, draft);
                continue;
            }
            processOrderService.saveFinishConfig(order.getUuid(), roll.getUuid(), readConfig(draft));
        }
        processOrderService.calcFee(order.getUuid());
        return submittedResult(order);
    }

    private Set<String> coveredByMultiSourceDrafts(Map<String, ProcessConfigDraft> drafts) {
        Set<String> covered = new LinkedHashSet<>();
        for (ProcessConfigDraft draft : drafts.values()) {
            if (draft.getConfigStatus() == null || draft.getConfigStatus() != 1) {
                continue;
            }
            if (routeDraftManager.isRouteDraft(draft)) {
                continue;
            }
            FinishConfigSaveDTO config = readConfig(draft);
            if (config.getMainStepType() == null || config.getMainStepType() != FeeCalculator.STEP_TYPE_REWIND) {
                continue;
            }
            if (config.getRewindMode() == null || config.getRewindMode() != REWIND_MODE_MULTI_SOURCE) {
                continue;
            }
            collectMultiSourceRolls(config, draft.getOriginalUuid(), covered);
        }
        return covered;
    }

    private void collectMultiSourceRolls(FinishConfigSaveDTO config, String ownerUuid, Set<String> covered) {
        if (config.getRewindSegments() != null && !config.getRewindSegments().isEmpty()) {
            for (RewindPlanPreviewDTO.RewindSegmentDTO segment : config.getRewindSegments()) {
                collectSources(segment.getSources(), ownerUuid, covered);
            }
            return;
        }
        for (FinishConfigSpecDTO spec : config.getFinishSpecs() == null ? List.<FinishConfigSpecDTO>of() : config.getFinishSpecs()) {
            collectSources(spec.getSources(), ownerUuid, covered);
        }
    }

    private void collectSources(List<FinishConfigSpecDTO.FinishSourceDTO> sources,
                                String ownerUuid,
                                Set<String> covered) {
        for (FinishConfigSpecDTO.FinishSourceDTO source : sources == null ? List.<FinishConfigSpecDTO.FinishSourceDTO>of() : sources) {
            if (source.getOriginalUuid() != null && !source.getOriginalUuid().equals(ownerUuid)) {
                covered.add(source.getOriginalUuid());
            }
        }
    }

    private ProcessOrderSubmitVO submittedShell(ProcessOrder order) {
        ProcessOrderSubmitVO vo = new ProcessOrderSubmitVO();
        vo.setOrderUuid(order.getUuid());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderStatus(order.getOrderStatus());
        return vo;
    }

    private ProcessOrderSubmitVO submittedResult(ProcessOrder order) {
        ProcessOrderSubmitVO vo = submittedShell(order);
        List<FinishRoll> rolls = finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, order.getUuid())
                .ne(FinishRoll::getRollNoStatus, ROLL_NO_VOID)
                .orderByAsc(FinishRoll::getRowSort));
        for (FinishRoll roll : rolls) {
            if (roll.getIsSpare() != null && roll.getIsSpare() == IS_SPARE_YES) {
                vo.getSpareRollNos().add(roll.getFinishRollNo());
            } else {
                vo.getFinishRollNos().add(roll.getFinishRollNo());
            }
        }
        return vo;
    }

    private FinishConfigSaveDTO readConfig(ProcessConfigDraft draft) {
        try {
            JsonNode node = objectMapper.readTree(draft.getConfigJson());
            if (node.has("segments") || !node.has("rewindSegments")) {
                ProcessPlanDTO plan = objectMapper.treeToValue(node, ProcessPlanDTO.class);
                return processPlanMapper.toSaveDto(plan);
            }
            return objectMapper.treeToValue(node, FinishConfigSaveDTO.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException("工艺配置草稿解析失败");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException("工艺配置草稿序列化失败");
        }
    }

    private String nextOrderNo(DraftOrderBaseDTO dto) {
        return documentNoService.next(NoRuleBizType.PROCESS_ORDER, dto.getOrderDate());
    }
}
