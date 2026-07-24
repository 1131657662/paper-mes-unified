package com.paper.mes.customer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.paper.mes.common.PageResult;
import com.paper.mes.customer.dto.CustomerQuery;
import com.paper.mes.customer.dto.CustomerSaveDTO;
import com.paper.mes.customer.dto.CustomerVO;
import com.paper.mes.customer.entity.Customer;

public interface CustomerService extends IService<Customer> {

    PageResult<CustomerVO> pageCustomers(CustomerQuery query);

    Customer getByUuid(String uuid);

    CustomerVO getProfile(String uuid);

    String create(CustomerSaveDTO dto);

    void update(String uuid, CustomerSaveDTO dto);

    void delete(String uuid);
}
