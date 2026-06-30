package com.paper.mes.system.config.service;

import com.paper.mes.common.PageResult;
import com.paper.mes.system.config.dto.DictItemQuery;
import com.paper.mes.system.config.dto.DictItemSaveDTO;
import com.paper.mes.system.config.entity.SysDictItem;

import java.util.List;

public interface SystemDictService {
    PageResult<SysDictItem> page(DictItemQuery query);

    List<SysDictItem> enabledByTypes(List<String> dictTypes);

    SysDictItem getByUuid(String uuid);

    String create(DictItemSaveDTO dto);

    void update(String uuid, DictItemSaveDTO dto);

    void delete(String uuid);
}
