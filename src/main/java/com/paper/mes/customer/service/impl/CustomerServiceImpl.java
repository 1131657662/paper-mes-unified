package com.paper.mes.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.PageResult;
import com.paper.mes.common.PageRequestBounds;
import com.paper.mes.customer.dto.CustomerQuery;
import com.paper.mes.customer.dto.CustomerSaveDTO;
import com.paper.mes.customer.dto.CustomerVO;
import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.mapper.CustomerMapper;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.customer.service.CustomerProcessPriceReader;
import com.paper.mes.customer.service.CustomerProcessPriceWriter;
import com.paper.mes.system.config.constant.NoRuleBizType;
import com.paper.mes.system.config.service.DocumentNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

    private static final int DEFAULT_SETTLE_TYPE = 2;
    private static final int DEFAULT_INVOICE = 2;

    private final DocumentNoService documentNoService;
    private final CustomerProcessPriceReader priceReader;
    private final CustomerProcessPriceWriter priceWriter;

    @Override
    public PageResult<CustomerVO> pageCustomers(CustomerQuery query) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(Customer::getCustomerCode, kw)
                    .or().like(Customer::getCustomerName, kw));
        }
        wrapper.orderByDesc(Customer::getCreateTime);
        Page<Customer> page = page(PageRequestBounds.of(query.getCurrent(), query.getSize()), wrapper);
        return priceReader.toPage(page);
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
    public CustomerVO getProfile(String uuid) {
        return priceReader.toView(getByUuid(uuid));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(CustomerSaveDTO dto) {
        var prices = priceWriter.normalize(dto.getProcessPrices());
        Customer customer = new Customer();
        BeanUtils.copyProperties(dto, customer);
        customer.setCustomerCode(documentNoService.next(NoRuleBizType.CUSTOMER, LocalDate.now()));
        applyDefaults(customer);
        save(customer);
        priceWriter.replace(customer.getUuid(), prices);
        return customer.getUuid();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String uuid, CustomerSaveDTO dto) {
        Customer existing = getByUuid(uuid);
        var prices = priceWriter.normalizeForUpdate(uuid, dto.getProcessPrices());
        Integer savedVersion = existing.getVersion();
        String savedCode = existing.getCustomerCode();
        BeanUtils.copyProperties(dto, existing);
        existing.setUuid(uuid);
        existing.setVersion(savedVersion);
        existing.setCustomerCode(keepCodeOrGenerate(savedCode, NoRuleBizType.CUSTOMER));
        applyDefaults(existing);
        ConcurrencyGuard.requireUpdated(updateById(existing));
        priceWriter.replace(uuid, prices);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String uuid) {
        getByUuid(uuid);
        priceWriter.remove(uuid);
        removeById(uuid);
    }

    private String keepCodeOrGenerate(String code, String bizType) {
        return StringUtils.hasText(code) ? code : documentNoService.next(bizType, LocalDate.now());
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
