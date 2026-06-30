package com.paper.mes.system.config.dto;

import com.paper.mes.system.config.entity.SysConfigItem;
import lombok.Data;

@Data
public class RuntimeConfigVO {
    private String configKey;
    private String configName;
    private String configValue;
    private String valueType;
    private String unit;

    public static RuntimeConfigVO from(SysConfigItem item) {
        RuntimeConfigVO vo = new RuntimeConfigVO();
        vo.setConfigKey(item.getConfigKey());
        vo.setConfigName(item.getConfigName());
        vo.setConfigValue(item.getConfigValue());
        vo.setValueType(item.getValueType());
        vo.setUnit(item.getUnit());
        return vo;
    }
}
