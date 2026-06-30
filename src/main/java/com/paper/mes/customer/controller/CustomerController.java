package com.paper.mes.customer.controller;

import com.paper.mes.common.PageResult;
import com.paper.mes.common.R;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
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
    @RequirePermission(Permissions.BASE_VIEW)
    public R<PageResult<Customer>> page(CustomerQuery query) {
        return R.success(customerService.pageCustomers(query));
    }

    @GetMapping("/{uuid}")
    @RequirePermission(Permissions.BASE_VIEW)
    public R<Customer> detail(@PathVariable String uuid) {
        return R.success(customerService.getByUuid(uuid));
    }

    @PostMapping
    @RequirePermission(Permissions.BASE_MANAGE)
    public R<String> create(@Valid @RequestBody CustomerSaveDTO dto) {
        return R.success(customerService.create(dto));
    }

    @PutMapping("/{uuid}")
    @RequirePermission(Permissions.BASE_MANAGE)
    public R<Void> update(@PathVariable String uuid, @Valid @RequestBody CustomerSaveDTO dto) {
        customerService.update(uuid, dto);
        return R.success();
    }

    @DeleteMapping("/{uuid}")
    @RequirePermission(Permissions.BASE_MANAGE)
    public R<Void> delete(@PathVariable String uuid) {
        customerService.delete(uuid);
        return R.success();
    }
}
