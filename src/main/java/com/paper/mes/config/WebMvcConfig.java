package com.paper.mes.config;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.permission.PermissionInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/** 上传文件映射：兼容已存 /files/** 路径，并通过拦截器要求登录后访问。 */
@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final FileStorageProperties properties;
    private final AuthInterceptor authInterceptor;
    private final PermissionInterceptor permissionInterceptor;

    public WebMvcConfig(FileStorageProperties properties, AuthInterceptor authInterceptor,
                        PermissionInterceptor permissionInterceptor) {
        this.properties = properties;
        this.authInterceptor = authInterceptor;
        this.permissionInterceptor = permissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login");
        String filePattern = fileUrlPattern();
        if (!filePattern.startsWith("/api/")) {
            registry.addInterceptor(authInterceptor).addPathPatterns(filePattern);
        }
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absoluteDir = Paths.get(properties.getDir()).toAbsolutePath().normalize().toString();
        String location = "file:" + absoluteDir + "/";
        registry.addResourceHandler(properties.getUrlPrefix() + "/**")
                .addResourceLocations(location);
    }

    private String fileUrlPattern() {
        String prefix = properties.getUrlPrefix();
        if (prefix == null || prefix.isBlank()) {
            prefix = "/files";
        }
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        return prefix.endsWith("/") ? prefix + "**" : prefix + "/**";
    }
}
