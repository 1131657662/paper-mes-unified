package com.paper.mes.processorder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DamageImageServiceTest {
    private final OriginalRollMapper rollMapper = mock(OriginalRollMapper.class);
    private final FileStorageService storage = mock(FileStorageService.class);
    private final DamageImageService service = new DamageImageService(rollMapper, storage, new ObjectMapper());

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "null"})
    void append_whenExistingReferencesAreCorrupt_rejectsWithoutOverwriting(String damageImages) {
        OriginalRoll roll = roll(damageImages);

        assertThatThrownBy(() -> service.append(roll, new MultipartFile[]{mock(MultipartFile.class)}))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("引用数据已损坏");
        verifyNoInteractions(storage, rollMapper);
    }

    @Test
    void append_whenLaterFileFails_deletesFilesStoredEarlier() {
        MultipartFile first = mock(MultipartFile.class);
        MultipartFile second = mock(MultipartFile.class);
        when(storage.store(first)).thenReturn("/api/files/damage/first.png");
        when(storage.store(second)).thenThrow(new BusinessException("保存失败"));

        assertThatThrownBy(() -> service.append(roll(null), new MultipartFile[]{first, second}))
                .isInstanceOf(BusinessException.class);
        verify(storage).delete("/api/files/damage/first.png");
        verifyNoInteractions(rollMapper);
    }

    @Test
    void append_whenDatabaseUpdateFails_deletesNewFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(storage.store(file)).thenReturn("/api/files/damage/new.png");
        when(rollMapper.updateById(org.mockito.ArgumentMatchers.any(OriginalRoll.class))).thenReturn(0);

        assertThatThrownBy(() -> service.append(roll(null), new MultipartFile[]{file}))
                .isInstanceOf(BusinessException.class);
        verify(storage).delete("/api/files/damage/new.png");
    }

    @Test
    void append_whenOuterTransactionRollsBack_deletesNewFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(storage.store(file)).thenReturn("/api/files/damage/new.png");
        when(rollMapper.updateById(org.mockito.ArgumentMatchers.any(OriginalRoll.class))).thenReturn(1);
        TransactionSynchronizationManager.initSynchronization();

        service.append(roll(null), new MultipartFile[]{file});
        verify(storage, never()).delete("/api/files/damage/new.png");
        TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(storage).delete("/api/files/damage/new.png");
    }

    private OriginalRoll roll(String damageImages) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid("roll-1");
        roll.setDamageImages(damageImages);
        return roll;
    }
}
