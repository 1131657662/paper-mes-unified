package com.paper.mes.ai.process.status;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class ProcessAiDialogueV2FeatureTest {

    @AfterEach
    void clearAuthentication() {
        AuthContextHolder.clear();
    }

    @Test
    void disabledFeatureRejectsNewRequests() {
        AiProperties properties = new AiProperties();
        ProcessAiDialogueV2Feature feature = new ProcessAiDialogueV2Feature(properties);

        assertThatThrownBy(() -> feature.requireEnabled("order-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI协作解析暂未对当前账号或加工单开放");
    }

    @Test
    void enabledOrderAllowlistAllowsOnlyListedOrders() {
        AiProperties properties = new AiProperties();
        properties.setProcessDialogueV2Enabled(true);
        properties.setProcessDialogueV2OrderAllowlist("order-2, order-3");
        ProcessAiDialogueV2Feature feature = new ProcessAiDialogueV2Feature(properties);

        assertThatCode(() -> feature.requireEnabled("order-2")).doesNotThrowAnyException();
        assertThatThrownBy(() -> feature.requireEnabled("order-1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void enabledUserAllowlistAllowsListedAuthenticatedUser() {
        AiProperties properties = new AiProperties();
        properties.setProcessDialogueV2Enabled(true);
        properties.setProcessDialogueV2UserAllowlist("user-2");
        AuthContextHolder.setCurrentUser(CurrentUser.builder()
                .uuid("user-2").username("operator").realName("操作员").roleCode("OPERATOR")
                .build());
        ProcessAiDialogueV2Feature feature = new ProcessAiDialogueV2Feature(properties);

        assertThatCode(() -> feature.requireEnabled("order-1")).doesNotThrowAnyException();
    }
}
