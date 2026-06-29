package com.paper.mes.processorder.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.config.FileStorageProperties;
import com.paper.mes.processorder.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final FileStorageProperties properties;

    public FileStorageServiceImpl(FileStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("单张图片不能超过 10MB");
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("仅允许上传 jpg/jpeg/png/webp/gif 图片");
        }

        String day = LocalDate.now().format(DAY_FMT);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String relativePath = "damage/" + day + "/" + fileName;

        Path baseDir = Paths.get(properties.getDir()).toAbsolutePath().normalize();
        Path target = baseDir.resolve(relativePath).normalize();
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw new BusinessException("图片保存失败");
        }

        return trimSlash(properties.getUrlPrefix()) + "/" + relativePath;
    }

    private String extensionOf(String originalName) {
        if (!StringUtils.hasText(originalName)) {
            return "";
        }
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(dot + 1).toLowerCase();
    }

    private String trimSlash(String prefix) {
        String p = StringUtils.hasText(prefix) ? prefix : "/files";
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        if (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }
}
