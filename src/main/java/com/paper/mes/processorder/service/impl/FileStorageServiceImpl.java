package com.paper.mes.processorder.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.config.FileStorageProperties;
import com.paper.mes.processorder.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
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
        validateContent(file, ext);

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

    private void validateContent(MultipartFile file, String extension) {
        byte[] header = new byte[12];
        int length;
        try (InputStream input = file.getInputStream()) {
            length = input.read(header);
        } catch (IOException e) {
            throw new BusinessException("图片读取失败");
        }
        if (!matchesImageSignature(header, length, extension)) {
            throw new BusinessException("图片内容与文件扩展名不匹配");
        }
    }

    private boolean matchesImageSignature(byte[] bytes, int length, String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> length >= 3 && unsigned(bytes[0]) == 0xFF
                    && unsigned(bytes[1]) == 0xD8 && unsigned(bytes[2]) == 0xFF;
            case "png" -> length >= 8 && unsigned(bytes[0]) == 0x89 && bytes[1] == 'P'
                    && bytes[2] == 'N' && bytes[3] == 'G';
            case "gif" -> length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F'
                    && bytes[3] == '8' && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a';
            case "webp" -> length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F'
                    && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E'
                    && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }

    private int unsigned(byte value) {
        return Byte.toUnsignedInt(value);
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
