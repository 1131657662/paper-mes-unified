package com.paper.mes.integration;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.RoleCodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

abstract class AuthenticatedBusinessFlowIT {

    @BeforeEach
    void authenticateBusinessFlow() {
        AuthContextHolder.setCurrentUser(CurrentUser.builder()
                .uuid("business-flow-admin")
                .username("business-flow-admin")
                .realName("业务流集成测试")
                .roleCode(RoleCodes.ADMIN)
                .build());
    }

    @AfterEach
    void clearBusinessFlowAuthentication() {
        AuthContextHolder.clear();
    }
}
