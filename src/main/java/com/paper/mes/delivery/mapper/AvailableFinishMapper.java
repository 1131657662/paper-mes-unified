package com.paper.mes.delivery.mapper;

import com.paper.mes.delivery.dto.AvailableFinishQuery;
import com.paper.mes.delivery.dto.AvailableFinishStatsVO;
import com.paper.mes.delivery.dto.AvailableFinishVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AvailableFinishMapper {

    long count(@Param("q") AvailableFinishQuery query);

    AvailableFinishStatsVO stats(@Param("q") AvailableFinishQuery query);

    List<AvailableFinishVO> rows(@Param("q") AvailableFinishQuery query,
                                 @Param("offset") long offset,
                                 @Param("limit") long limit);
}
