package com.paper.mes.config;

import com.paper.mes.auth.config.AuthInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 静态资源映射（P2-4）：将上传目录通过 {urlPrefix}/** 对外暴露，使已存破损图片可经 HTTP 访问。
 */
@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final FileStorageProperties properties;
    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(FileStorageProperties properties, AuthInterceptor authInterceptor) {
        this.properties = properties;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
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
}
