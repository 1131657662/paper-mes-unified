package com.paper.mes.report.mapper;

import com.paper.mes.report.dto.ReportDetailVO;
import com.paper.mes.report.dto.ReportDimensionVO;
import com.paper.mes.report.dto.ReportOverviewVO;
import com.paper.mes.report.dto.ReportQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 报表聚合查询：纯自定义 SQL（GROUP BY / JOIN），不继承 BaseMapper。SQL 见 ReportMapper.xml。
 */
@Mapper
public interface ReportMapper {

    ReportOverviewVO overview(@Param("q") ReportQuery q);

    List<ReportDimensionVO> dimensionSummary(@Param("q") ReportQuery q,
                                             @Param("dimension") String dimension);

    List<ReportDetailVO> detailRows(@Param("q") ReportQuery q);
}
