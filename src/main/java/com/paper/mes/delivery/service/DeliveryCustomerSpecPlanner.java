package com.paper.mes.delivery.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.customerdisplay.formula.*;
import com.paper.mes.delivery.dto.DeliveryCustomerSpecItemDTO;
import com.paper.mes.delivery.dto.DeliveryCustomerSpecVO;
import com.paper.mes.delivery.entity.DeliveryCustomerRevisionItem;
import com.paper.mes.processorder.entity.FinishRoll;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DeliveryCustomerSpecPlanner {

    private static final Set<RoundingMode> ROUNDING_MODES =
            Set.of(RoundingMode.HALF_UP, RoundingMode.UP, RoundingMode.DOWN);
    private final CustomerWeightFormulaEngine formulaEngine;

    public DeliveryCustomerSpecVO current(DeliveryCustomerSpecContext context) {
        DeliveryCustomerSpecVO row = physicalRow(context);
        applyPrevious(row, context);
        row.setCustomerPaperName(row.getPreviousCustomerPaperName());
        row.setCustomerGramWeight(row.getPreviousCustomerGramWeight());
        row.setCustomerFinishWidth(row.getPreviousCustomerFinishWidth());
        row.setCustomerDisplayWeight(row.getPreviousCustomerDisplayWeight());
        row.setCalculationMode(CustomerWeightCalculationMode.KEEP.name());
        markChanges(row);
        row.setValid(physicalValid(row));
        if (!row.isValid()) row.setError("出库实物规格或重量不完整");
        return row;
    }

    public DeliveryCustomerSpecVO plan(
            DeliveryCustomerSpecContext context, DeliveryCustomerSpecItemDTO item) {
        validate(context, item);
        DeliveryCustomerSpecVO row = physicalRow(context);
        applyPrevious(row, context);
        row.setCustomerPaperName(resolveName(item.getCustomerPaperName(), row.getPreviousCustomerPaperName()));
        row.setCustomerGramWeight(resolve(item.getCustomerGramWeight(), row.getPreviousCustomerGramWeight()));
        row.setCustomerFinishWidth(resolve(item.getCustomerFinishWidth(), row.getPreviousCustomerFinishWidth()));
        row.setCustomerRemark(resolveName(item.getCustomerRemark(), row.getCustomerRemark()));
        row.setCustomerDisplayWeight(calculateWeight(item, row));
        row.setCalculationMode(item.getCalculationMode().name());
        row.setValueSource("DRAFT");
        markChanges(row);
        row.setValid(true);
        return row;
    }

    private DeliveryCustomerSpecVO physicalRow(DeliveryCustomerSpecContext context) {
        var physical = context.physical();
        DeliveryCustomerSpecVO row = new DeliveryCustomerSpecVO();
        row.setDeliveryDetailUuid(physical.getUuid());
        row.setDetailVersion(context.detail().getVersion());
        row.setFinishUuid(physical.getFinishUuid());
        row.setFinishRollNo(physical.getFinishRollNo());
        row.setOrderUuid(physical.getOrderUuid());
        row.setOrderNo(physical.getOrderNo());
        row.setPhysicalPaperName(physical.getPaperName());
        row.setPhysicalGramWeight(physical.getGramWeight());
        row.setPhysicalFinishWidth(physical.getFinishWidth());
        row.setPhysicalDeliveryWeight(physical.getOutWeight());
        return row;
    }

    private void applyPrevious(DeliveryCustomerSpecVO row, DeliveryCustomerSpecContext context) {
        DeliveryCustomerRevisionItem revision = context.previousRevision();
        if (revision != null) {
            applyRevision(row, revision);
            return;
        }
        if (context.usePhysicalBaseline()) {
            applyPhysicalBaseline(row);
            return;
        }
        FinishRoll finish = context.finish();
        row.setPreviousCustomerPaperName(resolveName(finish.getCustomerPaperName(), row.getPhysicalPaperName()));
        row.setPreviousCustomerGramWeight(resolve(finish.getCustomerGramWeight(), row.getPhysicalGramWeight()));
        row.setPreviousCustomerFinishWidth(resolve(finish.getCustomerFinishWidth(), row.getPhysicalFinishWidth()));
        row.setPreviousCustomerDisplayWeight(finishDefaultWeight(finish, row.getPhysicalDeliveryWeight()));
        row.setValueSource(hasFinishOverride(finish) ? "FINISH_DEFAULT" : "PHYSICAL");
    }

    private void applyPhysicalBaseline(DeliveryCustomerSpecVO row) {
        row.setPreviousCustomerPaperName(row.getPhysicalPaperName());
        row.setPreviousCustomerGramWeight(row.getPhysicalGramWeight());
        row.setPreviousCustomerFinishWidth(row.getPhysicalFinishWidth());
        row.setPreviousCustomerDisplayWeight(row.getPhysicalDeliveryWeight());
        row.setValueSource("HISTORICAL_BASELINE");
    }

    private void applyRevision(DeliveryCustomerSpecVO row, DeliveryCustomerRevisionItem revision) {
        row.setPreviousCustomerPaperName(revision.getCustomerPaperName());
        row.setPreviousCustomerGramWeight(revision.getCustomerGramWeight());
        row.setPreviousCustomerFinishWidth(revision.getCustomerFinishWidth());
        row.setPreviousCustomerDisplayWeight(revision.getCustomerDisplayWeight());
        row.setCustomerRemark(revision.getCustomerRemark());
        row.setValueSource("DELIVERY_REVISION");
    }

    private BigDecimal finishDefaultWeight(FinishRoll finish, BigDecimal physicalDeliveryWeight) {
        BigDecimal customer = finish.getCustomerDisplayWeight();
        BigDecimal physicalFinish = finish.getActualWeight();
        if (physicalDeliveryWeight == null || customer == null
                || physicalFinish == null || physicalFinish.signum() <= 0) {
            return physicalDeliveryWeight;
        }
        return customer.multiply(physicalDeliveryWeight)
                .divide(physicalFinish, 3, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateWeight(DeliveryCustomerSpecItemDTO item, DeliveryCustomerSpecVO row) {
        validateRounding(item.getRoundingMode());
        BigDecimal physical = requirePositive(row.getPhysicalDeliveryWeight(), "实物出库重量");
        BigDecimal result = switch (item.getCalculationMode()) {
            case KEEP -> requirePositive(row.getPreviousCustomerDisplayWeight(), "原客户重量");
            case FIXED, MANUAL -> requirePositive(item.getCustomerDisplayWeight(), "客户单据重量");
            case DELTA -> physical.add(requireOperand(item));
            case RATIO -> physical.multiply(requireOperand(item));
            case FORMULA -> formulaWeight(item, row, physical);
        };
        return requirePositive(result, "客户单据重量")
                .setScale(item.getRoundingScale(), item.getRoundingMode());
    }

    private BigDecimal formulaWeight(
            DeliveryCustomerSpecItemDTO item, DeliveryCustomerSpecVO row, BigDecimal physical) {
        Map<String, BigDecimal> variables = new HashMap<>();
        if (item.getFormulaVariables() != null) variables.putAll(item.getFormulaVariables());
        variables.put("physicalWeight", physical);
        variables.put("finishWeight", physical);
        variables.put("physicalGsm", decimal(row.getPhysicalGramWeight(), "实物克重"));
        variables.put("physicalWidth", decimal(row.getPhysicalFinishWidth(), "实物门幅"));
        variables.put("finishWidth", decimal(row.getPhysicalFinishWidth(), "实物门幅"));
        variables.put("customerGsm", decimal(row.getCustomerGramWeight(), "客户克重"));
        variables.put("customerWidth", decimal(row.getCustomerFinishWidth(), "客户门幅"));
        CustomerWeightFormulaResult result = formulaEngine.evaluate(new CustomerWeightFormulaRequest(
                item.getFormulaExpression(), variables, item.getRoundingScale(),
                item.getRoundingMode(), item.getZeroPolicy()));
        return result.status() == CustomerWeightFormulaResult.Status.SKIPPED_ZERO
                ? row.getPreviousCustomerDisplayWeight() : result.roundedValue();
    }

    private void validate(DeliveryCustomerSpecContext context, DeliveryCustomerSpecItemDTO item) {
        if (!Objects.equals(context.detail().getVersion(), item.getExpectedDetailVersion())) {
            throw new BusinessException("出库明细已被修改，请刷新后重试：" + context.physical().getFinishRollNo());
        }
        if (!physicalValid(physicalRow(context))) throw new BusinessException("出库实物规格或重量不完整");
    }

    private void markChanges(DeliveryCustomerSpecVO row) {
        row.setSpecificationChanged(!Objects.equals(row.getPhysicalPaperName(), row.getCustomerPaperName())
                || !Objects.equals(row.getPhysicalGramWeight(), row.getCustomerGramWeight())
                || !Objects.equals(row.getPhysicalFinishWidth(), row.getCustomerFinishWidth()));
        row.setWeightChanged(row.getCustomerDisplayWeight() != null && row.getPhysicalDeliveryWeight() != null
                && row.getCustomerDisplayWeight().compareTo(row.getPhysicalDeliveryWeight()) != 0);
    }

    private boolean physicalValid(DeliveryCustomerSpecVO row) {
        return StringUtils.hasText(row.getPhysicalPaperName()) && positive(row.getPhysicalGramWeight())
                && positive(row.getPhysicalFinishWidth())
                && row.getPhysicalDeliveryWeight() != null && row.getPhysicalDeliveryWeight().signum() > 0;
    }

    private boolean hasFinishOverride(FinishRoll finish) {
        return StringUtils.hasText(finish.getCustomerPaperName()) || finish.getCustomerGramWeight() != null
                || finish.getCustomerFinishWidth() != null || finish.getCustomerDisplayWeight() != null;
    }

    private void validateRounding(RoundingMode mode) {
        if (!ROUNDING_MODES.contains(mode)) throw new BusinessException("客户重量舍入方式不受支持");
    }

    private BigDecimal requireOperand(DeliveryCustomerSpecItemDTO item) {
        if (item.getWeightOperand() == null) throw new BusinessException("重量计算参数不能为空");
        return item.getWeightOperand();
    }

    private BigDecimal requirePositive(BigDecimal value, String label) {
        if (value == null || value.signum() <= 0) throw new BusinessException(label + "必须大于0");
        return value;
    }

    private BigDecimal decimal(Integer value, String label) {
        if (!positive(value)) throw new BusinessException(label + "必须大于0");
        return BigDecimal.valueOf(value);
    }

    private boolean positive(Integer value) { return value != null && value > 0; }
    private String resolveName(String value, String fallback) { return StringUtils.hasText(value) ? value.trim() : fallback; }
    private <T> T resolve(T value, T fallback) { return value == null ? fallback : value; }
}
