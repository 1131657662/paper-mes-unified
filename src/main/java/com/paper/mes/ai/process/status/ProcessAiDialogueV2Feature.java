package com.paper.mes.ai.process.status;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Server-side rollout gate for new collaborative process dialogue requests. */
@Component
@RequiredArgsConstructor
public class ProcessAiDialogueV2Feature {

    private final AiProperties properties;

    public void requireEnabled(String orderUuid) {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        String userUuid = user == null ? null : user.getUuid();
        if (!enabledFor(orderUuid, userUuid)) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE,
                    "AI_PROCESS_DIALOGUE_V2_DISABLED", "AI协作解析暂未对当前账号或加工单开放");
        }
    }

    boolean enabledFor(String orderUuid, String userUuid) {
        if (!properties.isProcessDialogueV2Enabled()) return false;
        Set<String> orders = values(properties.getProcessDialogueV2OrderAllowlist());
        Set<String> users = values(properties.getProcessDialogueV2UserAllowlist());
        if (orders.isEmpty() && users.isEmpty()) return true;
        return (orderUuid != null && orders.contains(orderUuid))
                || (userUuid != null && users.contains(userUuid));
    }

    private Set<String> values(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
