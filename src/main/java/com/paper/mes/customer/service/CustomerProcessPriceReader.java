package com.paper.mes.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.paper.mes.common.PageResult;
import com.paper.mes.customer.dto.CustomerProcessPriceVO;
import com.paper.mes.customer.dto.CustomerVO;
import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.entity.CustomerProcessPrice;
import com.paper.mes.customer.mapper.CustomerProcessPriceMapper;
import com.paper.mes.processorder.dto.ProcessCatalogVO;
import com.paper.mes.processorder.service.ProcessCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CustomerProcessPriceReader {

    private final CustomerProcessPriceMapper priceMapper;
    private final ProcessCatalogService catalogService;

    public PageResult<CustomerVO> toPage(Page<Customer> page) {
        PageResult<CustomerVO> result = new PageResult<>();
        result.setRecords(toViews(page.getRecords()));
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }

    public CustomerVO toView(Customer customer) {
        return toViews(List.of(customer)).getFirst();
    }

    private List<CustomerVO> toViews(List<Customer> customers) {
        Map<String, List<CustomerProcessPriceVO>> prices = loadPrices(
                customers.stream().map(Customer::getUuid).toList());
        return customers.stream().map(customer -> toView(customer,
                prices.getOrDefault(customer.getUuid(), List.of()))).toList();
    }

    private Map<String, List<CustomerProcessPriceVO>> loadPrices(Collection<String> customerUuids) {
        if (customerUuids.isEmpty()) return Map.of();
        Map<String, ProcessCatalogVO> catalogs = catalogService.listActive().stream()
                .collect(Collectors.toMap(ProcessCatalogVO::uuid, Function.identity()));
        return priceMapper.selectList(new LambdaQueryWrapper<CustomerProcessPrice>()
                        .in(CustomerProcessPrice::getCustomerUuid, customerUuids)
                        .orderByDesc(CustomerProcessPrice::getIsDefault)
                        .orderByAsc(CustomerProcessPrice::getBillingBasis))
                .stream().filter(row -> catalogs.containsKey(row.getCatalogUuid()))
                .collect(Collectors.groupingBy(CustomerProcessPrice::getCustomerUuid,
                        Collectors.mapping(row -> priceView(row, catalogs.get(row.getCatalogUuid())),
                                Collectors.toList())));
    }

    private CustomerProcessPriceVO priceView(CustomerProcessPrice row, ProcessCatalogVO catalog) {
        return new CustomerProcessPriceVO(row.getCatalogUuid(), catalog.stepType(), catalog.code(),
                catalog.name(), row.getBillingBasis(), unitName(row.getBillingBasis()), row.getPrice(),
                Integer.valueOf(1).equals(row.getIsDefault()));
    }

    private CustomerVO toView(Customer customer, List<CustomerProcessPriceVO> prices) {
        CustomerVO view = new CustomerVO();
        BeanUtils.copyProperties(customer, view);
        view.setProcessPrices(prices);
        return view;
    }

    private String unitName(String basis) {
        return switch (basis) {
            case "PIECE" -> "件";
            case "TON" -> "吨";
            default -> "单";
        };
    }
}
