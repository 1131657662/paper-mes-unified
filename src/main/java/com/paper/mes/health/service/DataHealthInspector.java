package com.paper.mes.health.service;

import com.paper.mes.health.dto.DataHealthIssueVO;

import java.util.List;

/** 单一业务域的数据健康检查器。 */
public interface DataHealthInspector {

    List<DataHealthIssueVO> inspect();
}
