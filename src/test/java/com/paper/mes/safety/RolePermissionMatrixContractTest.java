package com.paper.mes.safety;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.auth.permission.RoleCodes;
import com.paper.mes.delivery.controller.DeliveryController;
import com.paper.mes.processorder.controller.FinishRollController;
import com.paper.mes.processorder.controller.ProcessOrderController;
import com.paper.mes.processorder.controller.ProcessOrderDraftController;
import com.paper.mes.settle.controller.SettleController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RolePermissionMatrixContractTest {

    private static final List<Class<?>> PROCESS_CONTROLLERS = List.of(
            ProcessOrderController.class,
            ProcessOrderDraftController.class,
            FinishRollController.class);
    private static final List<Class<?>> CORE_CONTROLLERS = List.of(
            ProcessOrderController.class,
            ProcessOrderDraftController.class,
            FinishRollController.class,
            DeliveryController.class,
            SettleController.class);

    @Test
    void admin_canPerformAllCoreBusinessWrites() {
        assertThat(writeMethods(CORE_CONTROLLERS))
                .as("核心业务必须存在写接口")
                .isNotEmpty()
                .allMatch(method -> canAccess(RoleCodes.ADMIN, method));
    }

    @Test
    void orderClerk_canPerformProcessWritesExceptBackRecord() {
        writeMethods(PROCESS_CONTROLLERS).forEach(method -> {
            boolean expected = !method.getName().equals("backRecord");
            assertThat(canAccess(RoleCodes.ORDER_CLERK, method))
                    .as("制单员访问 %s", method)
                    .isEqualTo(expected);
        });
    }

    @Test
    void recorder_canOnlyPerformBackRecordAmongProcessWrites() {
        writeMethods(PROCESS_CONTROLLERS).forEach(method -> {
            boolean expected = method.getName().equals("backRecord");
            assertThat(canAccess(RoleCodes.RECORDER, method))
                    .as("回录员访问 %s", method)
                    .isEqualTo(expected);
        });
    }

    @Test
    void viewer_cannotPerformCoreBusinessWrites() {
        assertThat(writeMethods(CORE_CONTROLLERS))
                .as("核心业务必须存在写接口")
                .isNotEmpty()
                .allMatch(method -> !canAccess(RoleCodes.VIEWER, method));
    }

    @Test
    void warehouse_canManageDeliveryButCannotManageSettlement() {
        assertThat(canAccess(RoleCodes.WAREHOUSE, method(DeliveryController.class, "create"))).isTrue();
        assertThat(canAccess(RoleCodes.WAREHOUSE, method(DeliveryController.class, "confirm"))).isTrue();
        assertThat(canAccess(RoleCodes.WAREHOUSE, method(SettleController.class, "createByOrders"))).isFalse();
        assertThat(canAccess(RoleCodes.WAREHOUSE, method(SettleController.class, "receive"))).isFalse();
    }

    @Test
    void finance_canManageSettlementButCannotManageDelivery() {
        assertThat(canAccess(RoleCodes.FINANCE, method(SettleController.class, "createByOrders"))).isTrue();
        assertThat(canAccess(RoleCodes.FINANCE, method(SettleController.class, "receive"))).isTrue();
        assertThat(canAccess(RoleCodes.FINANCE, method(DeliveryController.class, "create"))).isFalse();
        assertThat(canAccess(RoleCodes.FINANCE, method(DeliveryController.class, "confirm"))).isFalse();
    }

    private List<Method> writeMethods(List<Class<?>> controllers) {
        return controllers.stream()
                .flatMap(controller -> Arrays.stream(controller.getDeclaredMethods()))
                .filter(this::isWriteMethod)
                .toList();
    }

    private boolean isWriteMethod(Method method) {
        return method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class);
    }

    private Method method(Class<?> controller, String name) {
        return Arrays.stream(controller.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private boolean canAccess(String roleCode, Method method) {
        RequirePermission required = method.getAnnotation(RequirePermission.class);
        if (required == null) {
            required = method.getDeclaringClass().getAnnotation(RequirePermission.class);
        }
        if (required == null) {
            return true;
        }
        List<String> owned = Permissions.resolve(roleCode);
        return owned.contains(Permissions.ALL)
                || Arrays.stream(required.value()).anyMatch(owned::contains);
    }
}
