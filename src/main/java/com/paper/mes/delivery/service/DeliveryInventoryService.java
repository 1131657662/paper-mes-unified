package com.paper.mes.delivery.service;

import com.paper.mes.common.PageResult;
import com.paper.mes.delivery.dto.DeliveryInventoryCustomerQuery;
import com.paper.mes.delivery.dto.DeliveryInventoryCustomerVO;
import com.paper.mes.delivery.dto.DeliveryInventoryAvailabilityRequest;
import com.paper.mes.delivery.dto.DeliveryInventoryAvailabilityVO;
import com.paper.mes.delivery.dto.DeliveryInventoryFilter;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishQuery;
import com.paper.mes.delivery.dto.DeliveryInventoryFinishVO;
import com.paper.mes.delivery.dto.DeliveryInventorySummaryVO;

public interface DeliveryInventoryService {

    DeliveryInventorySummaryVO summary(DeliveryInventoryFilter filter);

    PageResult<DeliveryInventoryCustomerVO> pageCustomers(DeliveryInventoryCustomerQuery query);

    PageResult<DeliveryInventoryFinishVO> pageFinishes(DeliveryInventoryFinishQuery query);

    DeliveryInventoryAvailabilityVO validateAvailability(DeliveryInventoryAvailabilityRequest request);

}
