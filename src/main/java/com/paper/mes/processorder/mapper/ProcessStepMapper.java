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

    @Select("""
            SELECT EXISTS (
              SELECT 1 FROM biz_process_stage_output output
              WHERE output.is_deleted = 0 AND output.step_uuid = #{stepUuid}
              UNION ALL
              SELECT 1 FROM biz_process_stage_input_rel input_rel
              WHERE input_rel.is_deleted = 0
                AND (input_rel.step_uuid = #{stepUuid} OR input_rel.source_step_uuid = #{stepUuid})
              UNION ALL
              SELECT 1 FROM biz_process_step child
              WHERE child.is_deleted = 0 AND child.parent_step_uuid = #{stepUuid}
            )
            """)
    boolean hasActiveRouteReferences(@Param("stepUuid") String stepUuid);
}
