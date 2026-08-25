package com.paper.mes.config;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.remain.config.RemainFeatureInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 上传文件映射：兼容已存 /files/** 路径，并通过拦截器要求登录后访问。 */
@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final RemainFeatureInterceptor remainFeatureInterceptor;
    private final PermissionInterceptor permissionInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor,
                        RemainFeatureInterceptor remainFeatureInterceptor,
                        PermissionInterceptor permissionInterceptor) {
        this.authInterceptor = authInterceptor;
        this.remainFeatureInterceptor = remainFeatureInterceptor;
        this.permissionInterceptor = permissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**", "/files/**");
        registry.addInterceptor(remainFeatureInterceptor)
                .addPathPatterns("/api/remain-registrations/**",
                        "/api/remain-adjustments/**", "/api/remain-refunds/**");
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/**", "/files/**");
    }
}
