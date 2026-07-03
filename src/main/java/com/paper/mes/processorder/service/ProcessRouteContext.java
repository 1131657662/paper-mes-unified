package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;

public record ProcessRouteContext(ProcessOrder order, OriginalRoll roll) {
}
