package com.paper.mes.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.PageResult;
import com.paper.mes.customer.dto.CustomerQuery;
import com.paper.mes.customer.dto.CustomerSaveDTO;
import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.mapper.CustomerMapper;
import com.paper.mes.customer.service.CustomerService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

    private static final int DEFAULT_SETTLE_TYPE = 2;
    private static final int DEFAULT_INVOICE = 2;

    @Override
    public PageResult<Customer> pageCustomers(CustomerQuery query) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(Customer::getCustomerCode, kw)
                    .or().like(Customer::getCustomerName, kw));
        }
        wrapper.orderByDesc(Customer::getCreateTime);
        Page<Customer> page = page(Page.of(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.of(page);
    }

    @Override
    public Customer getByUuid(String uuid) {
        Customer customer = getById(uuid);
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }
        return customer;
    }

    @Override
    public String create(CustomerSaveDTO dto) {
        ensureCodeUnique(dto.getCustomerCode(), null);
        Customer customer = new Customer();
        BeanUtils.copyProperties(dto, customer);
        applyDefaults(customer);
        save(customer);
        return customer.getUuid();
    }

    @Override
    public void update(String uuid, CustomerSaveDTO dto) {
        Customer existing = getByUuid(uuid);
        ensureCodeUnique(dto.getCustomerCode(), uuid);
        Integer savedVersion = existing.getVersion();
        BeanUtils.copyProperties(dto, existing);
        existing.setUuid(uuid);
        existing.setVersion(savedVersion);
        applyDefaults(existing);
        updateById(existing);
    }

    @Override
    public void delete(String uuid) {
        getByUuid(uuid);
        removeById(uuid);
    }

    private void ensureCodeUnique(String customerCode, String excludeUuid) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .eq(Customer::getCustomerCode, customerCode);
        if (StringUtils.hasText(excludeUuid)) {
            wrapper.ne(Customer::getUuid, excludeUuid);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException("客户编码已存在：" + customerCode);
        }
    }

    private void applyDefaults(Customer customer) {
        if (customer.getSettleType() == null) {
            customer.setSettleType(DEFAULT_SETTLE_TYPE);
        }
        if (customer.getDefaultInvoice() == null) {
            customer.setDefaultInvoice(DEFAULT_INVOICE);
        }
        if (customer.getSettleType() != DEFAULT_SETTLE_TYPE) {
            customer.setSettleDay(null);
        }
    }
}
