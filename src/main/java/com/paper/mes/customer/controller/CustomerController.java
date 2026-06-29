package com.paper.mes.customer.controller;

import com.paper.mes.common.PageResult;
import com.paper.mes.common.R;
import com.paper.mes.customer.dto.CustomerQuery;
import com.paper.mes.customer.dto.CustomerSaveDTO;
import com.paper.mes.customer.entity.Customer;
import com.paper.mes.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public R<PageResult<Customer>> page(CustomerQuery query) {
        return R.success(customerService.pageCustomers(query));
    }

    @GetMapping("/{uuid}")
    public R<Customer> detail(@PathVariable String uuid) {
        return R.success(customerService.getByUuid(uuid));
    }

    @PostMapping
    public R<String> create(@Valid @RequestBody CustomerSaveDTO dto) {
        return R.success(customerService.create(dto));
    }

    @PutMapping("/{uuid}")
    public R<Void> update(@PathVariable String uuid, @Valid @RequestBody CustomerSaveDTO dto) {
        customerService.update(uuid, dto);
        return R.success();
    }

    @DeleteMapping("/{uuid}")
    public R<Void> delete(@PathVariable String uuid) {
        customerService.delete(uuid);
        return R.success();
    }
}
