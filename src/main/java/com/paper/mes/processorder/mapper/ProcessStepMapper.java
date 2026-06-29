package com.paper.mes.processorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.paper.mes.processorder.entity.ProcessStep;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ProcessStepMapper extends BaseMapper<ProcessStep> {

    /**
     * 查询指定原纸卷的最大工序顺序号
     */
    @Select("SELECT MAX(step_sort) FROM biz_process_step " +
            "WHERE original_uuid = #{originalUuid} AND is_deleted = 0")
    Integer selectMaxStepOrder(@Param("originalUuid") String originalUuid);
}
