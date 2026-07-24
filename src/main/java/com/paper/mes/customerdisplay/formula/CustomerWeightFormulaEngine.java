package com.paper.mes.customerdisplay.formula;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.config.ExpressionConfiguration;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.parser.ParseException;
import com.paper.mes.common.BusinessException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CustomerWeightFormulaEngine {

    private static final int MAX_EXPRESSION_LENGTH = 500;
    private static final BigDecimal MAX_ABSOLUTE_INPUT = new BigDecimal("1000000000000");
    private static final Pattern SAFE_CHARACTERS = Pattern.compile("[A-Za-z0-9_+\\-*/%<>=!(),.\\s]+$");
    private static final Pattern FUNCTION_CALL = Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\s*\\(");
    private static final Set<String> ALLOWED_FUNCTIONS = Set.of(
            "ABS", "ROUND", "FLOOR", "CEILING", "MIN", "MAX", "IF");
    private static final ExpressionConfiguration CONFIGURATION = ExpressionConfiguration
            .defaultConfiguration().toBuilder()
            .mathContext(new MathContext(20, RoundingMode.HALF_UP))
            .arraysAllowed(false)
            .structuresAllowed(false)
            .binaryAllowed(false)
            .implicitMultiplicationAllowed(false)
            .singleQuoteStringLiteralsAllowed(false)
            .stripTrailingZeros(true)
            .build();

    public CustomerWeightFormulaResult evaluate(CustomerWeightFormulaRequest request) {
        validateRequest(request);
        try {
            Expression expression = new Expression(request.expression(), CONFIGURATION);
            Set<String> usedVariables = expression.getUsedVariables();
            validateUsedVariables(usedVariables, request.variables());
            CustomerWeightFormulaResult skipped = applyZeroPolicy(request, usedVariables);
            if (skipped != null) return skipped;
            EvaluationValue value = expression.withValues(request.variables()).evaluate();
            return calculatedResult(request, usedVariables, value);
        } catch (ParseException | EvaluationException exception) {
            throw new BusinessException("客户重量公式无法计算：" + exception.getMessage());
        }
    }

    private void validateRequest(CustomerWeightFormulaRequest request) {
        if (request == null || request.expression() == null || request.expression().isBlank()) {
            throw new BusinessException("客户重量公式不能为空");
        }
        String expression = request.expression();
        if (expression.length() > MAX_EXPRESSION_LENGTH || !SAFE_CHARACTERS.matcher(expression).matches()) {
            throw new BusinessException("客户重量公式包含不允许的字符或长度超过500");
        }
        if (request.roundingScale() < 0 || request.roundingScale() > 3) {
            throw new BusinessException("客户重量保留小数位只能是0到3位");
        }
        if (request.roundingMode() == null || request.zeroPolicy() == null) {
            throw new BusinessException("客户重量舍入和0值策略不能为空");
        }
        validateFunctions(expression);
        validateInputs(request.variables());
    }

    private void validateFunctions(String expression) {
        Matcher matcher = FUNCTION_CALL.matcher(expression);
        while (matcher.find()) {
            String function = matcher.group(1).toUpperCase(Locale.ROOT);
            if (!ALLOWED_FUNCTIONS.contains(function)) {
                throw new BusinessException("客户重量公式不允许使用函数：" + function);
            }
        }
    }

    private void validateInputs(Map<String, BigDecimal> variables) {
        if (variables == null || variables.isEmpty()) {
            throw new BusinessException("客户重量公式参数不能为空");
        }
        for (Map.Entry<String, BigDecimal> entry : variables.entrySet()) {
            if (!CustomerWeightFormulaVariables.ALLOWED.contains(entry.getKey())) {
                throw new BusinessException("客户重量公式参数不受支持：" + entry.getKey());
            }
            BigDecimal value = entry.getValue();
            if (value == null || value.abs().compareTo(MAX_ABSOLUTE_INPUT) > 0) {
                throw new BusinessException("客户重量公式参数无效：" + entry.getKey());
            }
        }
    }

    private void validateUsedVariables(Set<String> usedVariables, Map<String, BigDecimal> inputs) {
        Set<String> missing = new LinkedHashSet<>(usedVariables);
        missing.removeAll(inputs.keySet());
        if (!missing.isEmpty()) {
            throw new BusinessException("客户重量公式缺少参数：" + String.join(", ", missing));
        }
    }

    private CustomerWeightFormulaResult applyZeroPolicy(
            CustomerWeightFormulaRequest request, Set<String> usedVariables) {
        boolean containsZero = usedVariables.stream()
                .map(request.variables()::get)
                .anyMatch(value -> value.signum() == 0);
        if (!containsZero || request.zeroPolicy() == CustomerWeightZeroPolicy.USE_ZERO) return null;
        if (request.zeroPolicy() == CustomerWeightZeroPolicy.ERROR) {
            throw new BusinessException("客户重量公式使用的参数中存在0值");
        }
        return new CustomerWeightFormulaResult(
                CustomerWeightFormulaResult.Status.SKIPPED_ZERO, null, null, Set.copyOf(usedVariables));
    }

    private CustomerWeightFormulaResult calculatedResult(
            CustomerWeightFormulaRequest request, Set<String> usedVariables, EvaluationValue value) {
        if (!value.isNumberValue()) throw new BusinessException("客户重量公式结果必须是数值");
        BigDecimal raw = value.getNumberValue();
        if (raw.signum() <= 0) throw new BusinessException("客户重量公式结果必须大于0");
        BigDecimal rounded = raw.setScale(request.roundingScale(), request.roundingMode());
        return new CustomerWeightFormulaResult(
                CustomerWeightFormulaResult.Status.CALCULATED, raw, rounded, Set.copyOf(usedVariables));
    }
}
