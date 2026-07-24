package com.paper.mes.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.customer.dto.CustomerProcessPriceSaveDTO;
import com.paper.mes.customer.entity.CustomerProcessPrice;
import com.paper.mes.customer.mapper.CustomerProcessPriceMapper;
import com.paper.mes.processorder.dto.ProcessCatalogVO;
import com.paper.mes.processorder.service.ProcessCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CustomerProcessPriceWriter {

    private final CustomerProcessPriceMapper priceMapper;
    private final ProcessCatalogService catalogService;

    public List<CustomerProcessPriceSaveDTO> normalize(List<CustomerProcessPriceSaveDTO> requested) {
        List<CustomerProcessPriceSaveDTO> values = requested == null ? List.of() : requested;
        validate(values);
        applyDefaultPerProcess(values);
        return values;
    }

    public List<CustomerProcessPriceSaveDTO> normalizeForUpdate(
            String customerUuid, List<CustomerProcessPriceSaveDTO> requested) {
        return requested == null ? currentPrices(customerUuid) : normalize(requested);
    }

    public void replace(String customerUuid, List<CustomerProcessPriceSaveDTO> prices) {
        remove(customerUuid);
        prices.forEach(price -> priceMapper.insert(toEntity(customerUuid, price)));
    }

    public void remove(String customerUuid) {
        priceMapper.delete(new LambdaQueryWrapper<CustomerProcessPrice>()
                .eq(CustomerProcessPrice::getCustomerUuid, customerUuid));
    }

    private void validate(List<CustomerProcessPriceSaveDTO> values) {
        Map<String, ProcessCatalogVO> catalogs = catalogIndex();
        Set<String> seen = new HashSet<>();
        for (CustomerProcessPriceSaveDTO value : values) {
            ProcessCatalogVO catalog = catalogs.get(value.getCatalogUuid());
            if (catalog == null || !"SERVICE_QUANTITY".equals(catalog.pricingStrategy())) {
                throw new BusinessException(ErrorCode.E003, "客户价格只能配置服务或包装工艺");
            }
            String basis = value.getBillingBasis();
            if (!"FIXED".equals(basis) && catalog.units().stream().noneMatch(unit -> unit.code().equals(basis))) {
                throw new BusinessException(ErrorCode.E003, "当前工艺不支持该计价口径");
            }
            if (!seen.add(value.getCatalogUuid() + ':' + basis)) {
                throw new BusinessException(ErrorCode.E003, "同一工艺的计价口径不能重复");
            }
        }
    }

    private void applyDefaultPerProcess(List<CustomerProcessPriceSaveDTO> values) {
        Map<String, List<CustomerProcessPriceSaveDTO>> grouped = values.stream()
                .collect(Collectors.groupingBy(CustomerProcessPriceSaveDTO::getCatalogUuid));
        for (List<CustomerProcessPriceSaveDTO> group : grouped.values()) {
            long defaults = group.stream().filter(item -> Integer.valueOf(1).equals(item.getIsDefault())).count();
            if (defaults > 1) throw new BusinessException(ErrorCode.E003, "同一工艺只能设置一个默认价格方案");
            if (defaults == 0 && !group.isEmpty()) group.getFirst().setIsDefault(1);
        }
    }

    private List<CustomerProcessPriceSaveDTO> currentPrices(String customerUuid) {
        return priceMapper.selectList(new LambdaQueryWrapper<CustomerProcessPrice>()
                        .eq(CustomerProcessPrice::getCustomerUuid, customerUuid))
                .stream().map(this::toSaveDto).toList();
    }

    private CustomerProcessPriceSaveDTO toSaveDto(CustomerProcessPrice row) {
        CustomerProcessPriceSaveDTO dto = new CustomerProcessPriceSaveDTO();
        dto.setCatalogUuid(row.getCatalogUuid());
        dto.setBillingBasis(row.getBillingBasis());
        dto.setPrice(row.getPrice());
        dto.setIsDefault(row.getIsDefault());
        return dto;
    }

    private CustomerProcessPrice toEntity(String customerUuid, CustomerProcessPriceSaveDTO source) {
        CustomerProcessPrice row = new CustomerProcessPrice();
        row.setCustomerUuid(customerUuid);
        row.setCatalogUuid(source.getCatalogUuid());
        row.setBillingBasis(source.getBillingBasis());
        row.setPrice(source.getPrice());
        row.setIsDefault(Integer.valueOf(1).equals(source.getIsDefault()) ? 1 : 0);
        return row;
    }

    private Map<String, ProcessCatalogVO> catalogIndex() {
        return catalogService.listActive().stream()
                .collect(Collectors.toMap(ProcessCatalogVO::uuid, Function.identity()));
    }
}
