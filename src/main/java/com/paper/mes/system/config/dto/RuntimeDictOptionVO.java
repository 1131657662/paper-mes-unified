package com.paper.mes.system.config.dto;

import com.paper.mes.system.config.entity.SysDictItem;
import lombok.Data;

@Data
public class RuntimeDictOptionVO {
    private String dictType;
    private String itemCode;
    private String itemName;
    private Integer itemValue;
    private Integer sortNo;
    private String remark;

    public static RuntimeDictOptionVO from(SysDictItem item) {
        RuntimeDictOptionVO vo = new RuntimeDictOptionVO();
        vo.setDictType(item.getDictType());
        vo.setItemCode(item.getItemCode());
        vo.setItemName(item.getItemName());
        vo.setItemValue(item.getItemValue());
        vo.setSortNo(item.getSortNo());
        vo.setRemark(item.getRemark());
        return vo;
    }
}
