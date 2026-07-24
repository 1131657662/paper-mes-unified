package com.paper.mes.report.mapper;

import com.paper.mes.report.dto.ReportCollectionAnalysisVO;
import com.paper.mes.report.dto.ReportDeliveryAnalysisVO;
import com.paper.mes.report.dto.ReportInventoryAnalysisVO;
import com.paper.mes.report.dto.ReportSettlementAnalysisVO;
import com.paper.mes.report.dto.ReportQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportOperationalMapper {
    ReportSettlementAnalysisVO.Overview settlementOverview(@Param("q") ReportQuery query);
    List<ReportSettlementAnalysisVO.Dimension> settlementMonthly(@Param("q") ReportQuery query);
    List<ReportSettlementAnalysisVO.Dimension> settlementCustomers(@Param("q") ReportQuery query);

    ReportCollectionAnalysisVO.Overview collectionOverview(@Param("q") ReportQuery query);
    List<ReportCollectionAnalysisVO.Dimension> collectionMonthly(@Param("q") ReportQuery query);
    List<ReportCollectionAnalysisVO.Dimension> collectionCustomers(@Param("q") ReportQuery query);

    ReportInventoryAnalysisVO.Overview inventoryOverview(@Param("q") ReportQuery query);
    List<ReportInventoryAnalysisVO.Dimension> inventoryMonthly(@Param("q") ReportQuery query);
    List<ReportInventoryAnalysisVO.Dimension> inventoryWarehouses(@Param("q") ReportQuery query);

    ReportDeliveryAnalysisVO.Overview deliveryOverview(@Param("q") ReportQuery query);
    List<ReportDeliveryAnalysisVO.Dimension> deliveryMonthly(@Param("q") ReportQuery query);
    List<ReportDeliveryAnalysisVO.Dimension> deliveryWarehouses(@Param("q") ReportQuery query);
}
