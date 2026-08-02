package com.paper.mes.processorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.settle.dto.SettleCandidateOrder;
import com.paper.mes.settle.dto.SettleCandidateQuery;
import org.apache.ibatis.annotations.Param;

public interface ProcessOrderMapper extends BaseMapper<ProcessOrder> {

    IPage<SettleCandidateOrder> selectSettleCandidates(
            Page<?> page, @Param("query") SettleCandidateQuery query);
}
