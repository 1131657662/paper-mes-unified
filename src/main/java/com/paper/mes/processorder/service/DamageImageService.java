package com.paper.mes.processorder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DamageImageService {
    private final OriginalRollMapper originalRollMapper;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    public List<String> append(OriginalRoll roll, MultipartFile[] files) {
        List<String> existing = parseExisting(roll.getDamageImages());
        List<String> stored = storeFiles(files);
        boolean rollbackCleanupRegistered = registerRollbackCleanup(stored);
        try {
            List<String> images = new ArrayList<>(existing);
            images.addAll(stored);
            roll.setDamageImages(serialize(images));
            ConcurrencyGuard.requireRowUpdated(originalRollMapper.updateById(roll));
            return images;
        } catch (RuntimeException exception) {
            if (!rollbackCleanupRegistered) cleanup(stored);
            throw exception;
        }
    }

    private List<String> parseExisting(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try {
            List<String> images = objectMapper.readValue(json, new TypeReference<>() {});
            if (images == null) throw new BusinessException("破损图片引用数据已损坏，请修复后再上传");
            return images;
        } catch (JsonProcessingException exception) {
            throw new BusinessException("破损图片引用数据已损坏，请修复后再上传");
        }
    }

    private List<String> storeFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BusinessException("未上传任何图片");
        }
        List<String> stored = new ArrayList<>();
        try {
            for (MultipartFile file : files) stored.add(fileStorageService.store(file));
            return stored;
        } catch (RuntimeException exception) {
            cleanup(stored);
            throw exception;
        }
    }

    private String serialize(List<String> images) {
        try {
            return objectMapper.writeValueAsString(images);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("图片路径序列化失败");
        }
    }

    private boolean registerRollbackCleanup(List<String> stored) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return false;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) cleanup(stored);
            }
        });
        return true;
    }

    private void cleanup(List<String> stored) {
        for (String fileUrl : stored) {
            try {
                fileStorageService.delete(fileUrl);
            } catch (RuntimeException exception) {
                log.warn("Failed to clean up damage image {}", fileUrl, exception);
            }
        }
    }
}
