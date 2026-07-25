package com.paper.mes.system.config.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.system.config.dto.DictItemSaveDTO;
import com.paper.mes.system.config.entity.SysDictItem;

import java.util.Objects;

final class BuiltInDictMetadataGuard {

    private BuiltInDictMetadataGuard() {
    }

    static void ensureUnchanged(SysDictItem item, DictItemSaveDTO dto) {
        boolean changed = !Objects.equals(item.getDictType(), dto.getDictType())
                || !Objects.equals(item.getDictName(), dto.getDictName())
                || !Objects.equals(item.getItemCode(), dto.getItemCode())
                || !Objects.equals(item.getItemValue(), dto.getItemValue());
        if (changed) {
            throw new BusinessException("内置字典项不允许修改分类、编码或枚举值");
        }
    }
}
