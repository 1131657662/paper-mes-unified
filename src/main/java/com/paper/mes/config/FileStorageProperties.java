package com.paper.mes.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 本地文件上传配置（P2-4）。dir 为落盘根目录，urlPrefix 为对外 HTTP 访问前缀。
 */
@Data
@ConfigurationProperties(prefix = "app.upload")
public class FileStorageProperties {

    /** 上传文件落盘根目录，默认项目运行目录下的 ./upload。 */
    private String dir = "./upload";

    /** 对外访问 URL 前缀，静态资源映射用，默认 /files。 */
    private String urlPrefix = "/api/files";
}
