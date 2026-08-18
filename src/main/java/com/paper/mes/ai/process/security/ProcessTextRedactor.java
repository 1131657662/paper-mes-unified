package com.paper.mes.ai.process.security;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ProcessTextRedactor {

    private static final Pattern UUID = Pattern.compile(
            "(?i)\\b[0-9a-f]{32}\\b|\\b[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}\\b");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}");
    private static final Pattern API_KEY = Pattern.compile(
            "(?i)(?:api[_ -]?key|token|bearer)\\s*[:=]?\\s*[A-Za-z0-9._-]{12,}");
    private static final Pattern ADDRESS = Pattern.compile(
            "(?i)(?:address|shipping[_ ]?address|\\u6536\\u8d27\\u5730\\u5740|"
                    + "\\u9001\\u8d27\\u5730\\u5740|\\u5730\\u5740)"
                    + "\\s*[:\\uFF1A=]\\s*[^,\\uFF0C;\\uFF1B\\r\\n]{4,120}");
    private static final Pattern CUSTOMER_IDENTITY = Pattern.compile(
            "(?i)(?:customer[_ ]?name|company|contact|consignee|alias|"
                    + "\\u5ba2\\u6237\\u540d\\u79f0|\\u5ba2\\u6237\\u540d|"
                    + "\\u516c\\u53f8\\u540d\\u79f0|\\u5355\\u4f4d\\u540d\\u79f0|"
                    + "\\u8054\\u7cfb\\u4eba|\\u6536\\u8d27\\u4eba|"
                    + "\\u7b80\\u79f0|\\u522b\\u540d)"
                    + "\\s*[:\\uFF1A=]?\\s*[\\p{L}\\p{N}()\\uFF08\\uFF09._\\-\\u00B7]{2,40}");
    private static final Pattern UNLABELED_ORGANIZATION = Pattern.compile(
            "(?:给|为|替|交给|发给)?\\s*[\\p{IsHan}A-Za-z0-9()（）·]{2,36}"
                    + "(?:有限责任公司|股份有限公司|有限公司|纸业|纸厂|包装|印刷|商贸|贸易|集团|公司|工厂)");
    private static final Pattern LONG_IDENTIFIER = Pattern.compile("(?<!\\d)\\d{6,}(?!\\d)");
    private static final Pattern PRICE = Pattern.compile(
            "(?<!\\d)(\\d+(?:\\.\\d{1,2})?)\\s*(?:元|块钱|块|¥)\\s*(每件|/件|每吨|/吨|固定)?");

    public ProcessTextRedactionResult redact(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "AI_PROCESS_TEXT_EMPTY", "请先填写客户加工要求");
        }
        List<ExtractedCharge> charges = new ArrayList<>();
        String sanitized = redactPrices(text, charges);
        sanitized = API_KEY.matcher(sanitized).replaceAll("[密钥已隐藏]");
        sanitized = EMAIL.matcher(sanitized).replaceAll("[邮箱已隐藏]");
        sanitized = PHONE.matcher(sanitized).replaceAll("[手机号已隐藏]");
        sanitized = ADDRESS.matcher(sanitized).replaceAll("[address redacted]");
        sanitized = CUSTOMER_IDENTITY.matcher(sanitized).replaceAll("[identity redacted]");
        sanitized = UNLABELED_ORGANIZATION.matcher(sanitized).replaceAll("[identity redacted]");
        sanitized = UUID.matcher(sanitized).replaceAll("[标识已隐藏]");
        sanitized = LONG_IDENTIFIER.matcher(sanitized).replaceAll("[编号已隐藏]");
        return new ProcessTextRedactionResult(sanitized, charges, !sanitized.equals(text));
    }

    private String redactPrices(String text, List<ExtractedCharge> charges) {
        Matcher matcher = PRICE.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String rawUnit = matcher.group(2);
            String unit = normalizeUnit(rawUnit);
            charges.add(new ExtractedCharge(new BigDecimal(matcher.group(1)), unit));
            String replacement = "[金额]" + (rawUnit == null ? "" : rawUnit);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String normalizeUnit(String rawUnit) {
        if (rawUnit == null) return "UNSPECIFIED";
        if (rawUnit.contains("件")) return "PIECE";
        if (rawUnit.contains("吨")) return "TON";
        return "FIXED";
    }

    public record ExtractedCharge(BigDecimal amount, String unit) {
    }
}
