package com.paper.mes.safety;

import com.paper.mes.auth.permission.AuthenticatedEndpoint;
import com.paper.mes.auth.permission.PublicEndpoint;
import com.paper.mes.auth.permission.RequirePermission;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EndpointPermissionPolicyContractTest {

    @Test
    void everyApiEndpointDeclaresItsAccessPolicy() throws ClassNotFoundException {
        List<String> missing = new ArrayList<>();
        for (Class<?> controller : controllers()) {
            for (Method method : controller.getDeclaredMethods()) {
                if (isEndpoint(method) && !hasPolicy(controller, method)) {
                    missing.add(controller.getSimpleName() + "." + method.getName());
                }
            }
        }
        assertThat(missing).as("未声明访问策略的接口").isEmpty();
    }

    private List<Class<?>> controllers() throws ClassNotFoundException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<Class<?>> controllers = new ArrayList<>();
        for (var candidate : scanner.findCandidateComponents("com.paper.mes")) {
            controllers.add(Class.forName(candidate.getBeanClassName()));
        }
        return controllers;
    }

    private boolean isEndpoint(Method method) {
        return AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class);
    }

    private boolean hasPolicy(Class<?> controller, Method method) {
        return hasPolicy(method, RequirePermission.class)
                || hasPolicy(method, AuthenticatedEndpoint.class)
                || hasPolicy(method, PublicEndpoint.class)
                || hasPolicy(controller, RequirePermission.class)
                || hasPolicy(controller, AuthenticatedEndpoint.class)
                || hasPolicy(controller, PublicEndpoint.class);
    }

    private boolean hasPolicy(java.lang.reflect.AnnotatedElement element,
                              Class<? extends java.lang.annotation.Annotation> annotation) {
        return AnnotatedElementUtils.hasAnnotation(element, annotation);
    }
}
