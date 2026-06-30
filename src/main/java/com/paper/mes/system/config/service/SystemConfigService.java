package com.paper.mes.system.config.service;

import com.paper.mes.common.PageResult;
import com.paper.mes.system.config.dto.ConfigItemQuery;
import com.paper.mes.system.config.dto.ConfigItemSaveDTO;
import com.paper.mes.system.config.entity.SysConfigItem;

import java.util.List;

public interface SystemConfigService {
    PageResult<SysConfigItem> page(ConfigItemQuery query);

    List<SysConfigItem> enabledByKeys(List<String> configKeys);

    SysConfigItem getByUuid(String uuid);

    String create(ConfigItemSaveDTO dto);

    void update(String uuid, ConfigItemSaveDTO dto);

    void delete(String uuid);
}
