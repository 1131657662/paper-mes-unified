package com.paper.mes.report.service;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;

@Component
public class ReportQueryScopePolicy {

    public CurrentUser currentUser() {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        if (user == null) throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        return user;
    }

    public String permissionHash(CurrentUser user) {
        String canonical = Permissions.resolve(user.getRoleCode()).stream()
                .sorted(Comparator.naturalOrder())
                .reduce((left, right) -> left + "|" + right)
                .orElse("");
        return sha256(canonical);
    }

    public String scopeHash(CurrentUser user, String permissionHash) {
        return sha256(user.getUuid() + "|" + permissionHash);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
