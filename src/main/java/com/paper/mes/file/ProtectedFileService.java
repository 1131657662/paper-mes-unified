package com.paper.mes.file;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class ProtectedFileService {

    private final FileStorageProperties properties;

    public ProtectedFile load(String rawPath) {
        Path path = resolvePath(rawPath);
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        }
        return new ProtectedFile(new FileSystemResource(path), contentType(path));
    }

    private Path resolvePath(String rawPath) {
        String relativePath = normalizeRelativePath(rawPath);
        Path baseDir = Paths.get(properties.getDir()).toAbsolutePath().normalize();
        Path path = baseDir.resolve(relativePath).normalize();
        if (!path.startsWith(baseDir)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件路径不合法");
        }
        return path;
    }

    private String normalizeRelativePath(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件路径不能为空");
        }
        String decoded = URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
        String normalized = decoded.replace('\\', '/').trim();
        normalized = stripPrefix(normalized, "/api/files/");
        normalized = stripPrefix(normalized, "/files/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件路径不能为空");
        }
        return normalized;
    }

    private String stripPrefix(String path, String prefix) {
        return path.startsWith(prefix) ? path.substring(prefix.length()) : path;
    }

    private String contentType(Path path) {
        try {
            String detected = Files.probeContentType(path);
            return StringUtils.hasText(detected) ? detected : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    public record ProtectedFile(Resource resource, String contentType) {
    }
}
