package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.customerdisplay.formula.CustomerWeightCalculationMode;
import com.paper.mes.customerdisplay.formula.CustomerWeightFormulaEngine;
import com.paper.mes.customerdisplay.formula.CustomerWeightFormulaRequest;
import com.paper.mes.customerdisplay.formula.CustomerWeightFormulaResult;
import com.paper.mes.processorder.dto.FinishCustomerSpecItemDTO;
import com.paper.mes.processorder.dto.FinishCustomerSpecVO;
import com.paper.mes.processorder.entity.FinishRoll;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FinishCustomerSpecPlanner {

    private static final Set<RoundingMode> ROUNDING_MODES =
            Set.of(RoundingMode.HALF_UP, RoundingMode.UP, RoundingMode.DOWN);
    private final CustomerWeightFormulaEngine formulaEngine;

    public FinishCustomerSpecVO current(FinishRoll finish) {
        FinishCustomerSpecVO row = physicalRow(finish);
        applyPreviousCustomer(row, finish);
        row.setCustomerPaperName(row.getPreviousCustomerPaperName());
        row.setCustomerGramWeight(row.getPreviousCustomerGramWeight());
        row.setCustomerFinishWidth(row.getPreviousCustomerFinishWidth());
        row.setCustomerDisplayWeight(row.getPreviousCustomerDisplayWeight());
        row.setCalculationMode(CustomerWeightCalculationMode.KEEP.name());
        row.setSpecificationChanged(specificationChanged(row));
        row.setWeightChanged(weightChanged(row));
        row.setValid(customerValuesValid(row));
        if (!row.isValid()) row.setError("客户品名、克重、门幅或重量不完整");
        return row;
    }

    public FinishCustomerSpecVO plan(FinishRoll finish, FinishCustomerSpecItemDTO item) {
        validateFinish(finish, item);
        FinishCustomerSpecVO row = physicalRow(finish);
        applyPreviousCustomer(row, finish);
        row.setCustomerPaperName(resolveName(item.getCustomerPaperName(), row.getPreviousCustomerPaperName()));
        row.setCustomerGramWeight(resolve(item.getCustomerGramWeight(), row.getPreviousCustomerGramWeight()));
        row.setCustomerFinishWidth(resolve(item.getCustomerFinishWidth(), row.getPreviousCustomerFinishWidth()));
        row.setCalculationMode(item.getCalculationMode().name());
        row.setCustomerDisplayWeight(calculateWeight(finish, item, row));
        validateCustomerValues(row);
        row.setSpecificationChanged(specificationChanged(row));
        row.setWeightChanged(weightChanged(row));
        row.setValid(true);
        return row;
    }

    private void validateFinish(FinishRoll finish, FinishCustomerSpecItemDTO item) {
        if (!Objects.equals(finish.getVersion(), item.getExpectedVersion())) {
            throw new BusinessException("成品已被其他人修改，请刷新后重试：" + finish.getFinishRollNo());
        }
        if (finish.getIsSpare() != null && finish.getIsSpare() == 1) {
            throw new BusinessException("备用卷号不能维护客户口径：" + finish.getFinishRollNo());
        }
        if (finish.getIsRemain() != null && finish.getIsRemain() == 1) {
            throw new BusinessException("切边或余料不能维护客户口径：" + finish.getFinishRollNo());
        }
        if (finish.getRollNoStatus() != null && finish.getRollNoStatus() == 3) {
            throw new BusinessException("已作废成品不能维护客户口径：" + finish.getFinishRollNo());
        }
    }

    private FinishCustomerSpecVO physicalRow(FinishRoll finish) {
        FinishCustomerSpecVO row = new FinishCustomerSpecVO();
        row.setFinishUuid(finish.getUuid());
        row.setFinishRollNo(finish.getFinishRollNo());
        row.setRowSort(finish.getRowSort());
        row.setFinishVersion(finish.getVersion());
        row.setPhysicalPaperName(finish.getPaperName());
        row.setPhysicalGramWeight(finish.getGramWeight());
        row.setPhysicalFinishWidth(finish.getFinishWidth());
        row.setPhysicalWeight(physicalWeight(finish));
        return row;
    }

    private void applyPreviousCustomer(FinishCustomerSpecVO row, FinishRoll finish) {
        row.setPreviousCustomerPaperName(resolveName(finish.getCustomerPaperName(), finish.getPaperName()));
        row.setPreviousCustomerGramWeight(resolve(finish.getCustomerGramWeight(), finish.getGramWeight()));
        row.setPreviousCustomerFinishWidth(resolve(finish.getCustomerFinishWidth(), finish.getFinishWidth()));
        row.setPreviousCustomerDisplayWeight(resolve(finish.getCustomerDisplayWeight(), physicalWeight(finish)));
    }

    private BigDecimal calculateWeight(
            FinishRoll finish, FinishCustomerSpecItemDTO item, FinishCustomerSpecVO row) {
        validateRounding(item.getRoundingMode());
        BigDecimal physical = physicalWeight(finish);
        BigDecimal result = switch (item.getCalculationMode()) {
            case KEEP -> resolve(row.getPreviousCustomerDisplayWeight(), physical);
            case FIXED, MANUAL -> requirePositive(item.getCustomerDisplayWeight(), "客户显示重量");
            case DELTA -> requirePhysical(physical).add(requireOperand(item));
            case RATIO -> requirePhysical(physical).multiply(requireOperand(item));
            case FORMULA -> formulaWeight(item, row, physical);
        };
        return requirePositive(result, "客户显示重量")
                .setScale(item.getRoundingScale(), item.getRoundingMode());
    }

    private BigDecimal formulaWeight(
            FinishCustomerSpecItemDTO item, FinishCustomerSpecVO row, BigDecimal physical) {
        Map<String, BigDecimal> variables = new HashMap<>();
        if (item.getFormulaVariables() != null) variables.putAll(item.getFormulaVariables());
        putPhysicalVariables(variables, row, physical);
        CustomerWeightFormulaResult result = formulaEngine.evaluate(new CustomerWeightFormulaRequest(
                item.getFormulaExpression(), variables, item.getRoundingScale(),
                item.getRoundingMode(), item.getZeroPolicy()));
        if (result.status() == CustomerWeightFormulaResult.Status.SKIPPED_ZERO) {
            return resolve(row.getPreviousCustomerDisplayWeight(), physical);
        }
        return result.roundedValue();
    }

    private void putPhysicalVariables(
            Map<String, BigDecimal> variables, FinishCustomerSpecVO row, BigDecimal physical) {
        if (physical != null) {
            variables.put("physicalWeight", physical);
            variables.put("finishWeight", physical);
        }
        putPositive(variables, "physicalGsm", row.getPhysicalGramWeight());
        putPositive(variables, "physicalWidth", row.getPhysicalFinishWidth());
        putPositive(variables, "finishWidth", row.getPhysicalFinishWidth());
        putPositive(variables, "customerGsm", row.getCustomerGramWeight());
        putPositive(variables, "customerWidth", row.getCustomerFinishWidth());
    }

    private boolean specificationChanged(FinishCustomerSpecVO row) {
        return !Objects.equals(row.getPhysicalPaperName(), row.getCustomerPaperName())
                || !Objects.equals(row.getPhysicalGramWeight(), row.getCustomerGramWeight())
                || !Objects.equals(row.getPhysicalFinishWidth(), row.getCustomerFinishWidth());
    }

    private boolean weightChanged(FinishCustomerSpecVO row) {
        if (row.getPhysicalWeight() == null || row.getCustomerDisplayWeight() == null) {
            return !Objects.equals(row.getPhysicalWeight(), row.getCustomerDisplayWeight());
        }
        return row.getCustomerDisplayWeight().compareTo(row.getPhysicalWeight()) != 0;
    }

    private boolean customerValuesValid(FinishCustomerSpecVO row) {
        return StringUtils.hasText(row.getCustomerPaperName())
                && positive(row.getCustomerGramWeight())
                && positive(row.getCustomerFinishWidth())
                && row.getCustomerDisplayWeight() != null
                && row.getCustomerDisplayWeight().signum() > 0;
    }

    private void validateCustomerValues(FinishCustomerSpecVO row) {
        if (!customerValuesValid(row)) throw new BusinessException("客户品名、克重、门幅和重量必须完整且大于0");
    }

    private void putPositive(Map<String, BigDecimal> variables, String name, Integer value) {
        if (positive(value)) variables.put(name, BigDecimal.valueOf(value));
    }

    private boolean positive(Integer value) {
        return value != null && value > 0;
    }

    private BigDecimal physicalWeight(FinishRoll finish) {
        return finish.getActualWeight() == null ? finish.getEstimateWeight() : finish.getActualWeight();
    }

    private BigDecimal requirePhysical(BigDecimal value) {
        return requirePositive(value, "成品物理重量");
    }

    private BigDecimal requireOperand(FinishCustomerSpecItemDTO item) {
        if (item.getWeightOperand() == null) throw new BusinessException("重量计算参数不能为空");
        return item.getWeightOperand();
    }

    private BigDecimal requirePositive(BigDecimal value, String label) {
        if (value == null || value.signum() <= 0) throw new BusinessException(label + "必须大于0");
        return value;
    }

    private void validateRounding(RoundingMode mode) {
        if (!ROUNDING_MODES.contains(mode)) throw new BusinessException("客户重量舍入方式不受支持");
    }

    private String resolveName(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred.trim() : fallback;
    }

    private <T> T resolve(T preferred, T fallback) {
        return preferred == null ? fallback : preferred;
    }
}
