package com.paper.mes.ai.security;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class AiQuestionSanitizer {

    private static final Pattern UUID = Pattern.compile("\\b[0-9a-fA-F]{32}\\b|\\b[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}\\b");
    private static final Pattern PHONE = Pattern.compile("\\b1[3-9]\\d{9}\\b");
    private static final Pattern EMAIL = Pattern.compile("\\b[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern LONG_NUMBER = Pattern.compile("\\b\\d{6,}\\b");
    private static final Set<String> SENSITIVE_MARKERS = Set.of(
            "password", "token", "cookie", "jwt", "api key", "api_key", "select ", "drop table",
            "shell", "sql", "密码", "令牌", "完整日志", "服务器命令");

    public Result inspect(String question) {
        if (question == null || question.isBlank()) {
            return new Result(false, "问题为空", "");
        }
        String normalized = question.toLowerCase(Locale.ROOT);
        if (SENSITIVE_MARKERS.stream().anyMatch(normalized::contains)
                || UUID.matcher(question).find() || PHONE.matcher(question).find()
                || LONG_NUMBER.matcher(question).find()
                || EMAIL.matcher(question).find()) {
            return new Result(false, "问题包含不允许出域的敏感信息", "");
        }
        String sanitized = EMAIL.matcher(question).replaceAll("[邮箱已隐藏]");
        sanitized = PHONE.matcher(sanitized).replaceAll("[手机号已隐藏]");
        sanitized = UUID.matcher(sanitized).replaceAll("[标识已隐藏]");
        return new Result(true, "", sanitized);
    }

    public record Result(boolean allowed, String reason, String sanitizedQuestion) {
    }
}
