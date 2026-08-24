package com.paper.mes.remain.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import com.paper.mes.remain.dto.RemainRegistrationCreateDTO;
import com.paper.mes.remain.dto.RemainRegistrationQuery;
import com.paper.mes.remain.dto.RemainRegistrationVO;
import com.paper.mes.remain.dto.RemainInventoryQuery;
import com.paper.mes.remain.dto.RemainInventoryVO;
import com.paper.mes.remain.dto.RemainRollbackDTO;
import com.paper.mes.remain.dto.RemainPriceConfirmDTO;
import com.paper.mes.remain.dto.RemainApplicationCreateDTO;
import com.paper.mes.remain.dto.RemainApplicationVO;
import com.paper.mes.remain.dto.RemainApplicationReverseDTO;
import com.paper.mes.remain.dto.RemainSaleCreateDTO;
import com.paper.mes.remain.dto.RemainSaleReverseDTO;
import com.paper.mes.remain.entity.RemainSale;
import com.paper.mes.remain.service.RemainRegistrationService;
import com.paper.mes.remain.service.RemainInventoryQueryService;
import com.paper.mes.remain.service.RemainPriceCommandService;
import com.paper.mes.remain.service.RemainApplicationCommandService;
import com.paper.mes.remain.service.RemainApplicationQueryService;
import com.paper.mes.remain.service.RemainApplicationReverseService;
import com.paper.mes.remain.service.RemainSaleCommandService;
import com.paper.mes.remain.service.RemainSaleReverseService;
import com.paper.mes.remain.service.RemainSaleQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/remain-registrations")
@RequiredArgsConstructor
public class RemainRegistrationController {

    private final RemainRegistrationService registrationService;
    private final RemainInventoryQueryService inventoryQueryService;
    private final RemainPriceCommandService priceCommandService;
    private final RemainApplicationCommandService applicationCommandService;
    private final RemainApplicationQueryService applicationQueryService;
    private final RemainApplicationReverseService applicationReverseService;
    private final RemainSaleCommandService saleCommandService;
    private final RemainSaleReverseService saleReverseService;
    private final RemainSaleQueryService saleQueryService;

    @PostMapping
    @RequirePermission(Permissions.REMAIN_REGISTER)
    public R<RemainRegistrationVO> create(@Valid @RequestBody RemainRegistrationCreateDTO request) {
        return R.success(registrationService.create(request));
    }

    @GetMapping
    @RequirePermission(Permissions.REMAIN_VIEW)
    public R<List<RemainRegistrationVO>> list(@Valid RemainRegistrationQuery query) {
        return R.success(registrationService.list(query));
    }

    @GetMapping("/inventory")
    @RequirePermission(Permissions.REMAIN_VIEW)
    public R<List<RemainInventoryVO>> inventory(@Valid RemainInventoryQuery query) {
        return R.success(inventoryQueryService.list(query));
    }

    @GetMapping("/{uuid}")
    @RequirePermission(Permissions.REMAIN_VIEW)
    public R<RemainRegistrationVO> detail(@PathVariable String uuid) {
        return R.success(registrationService.detail(uuid));
    }

    @PostMapping("/{uuid}/price")
    @RequirePermission(Permissions.REMAIN_PRICE)
    public R<RemainRegistrationVO> confirmPrice(@PathVariable String uuid,
                                                @Valid @RequestBody RemainPriceConfirmDTO request) {
        priceCommandService.confirm(uuid, request);
        return R.success(registrationService.detail(uuid));
    }

    @PostMapping("/{uuid}/applications")
    @RequirePermission(Permissions.SETTLE_RECEIVE)
    public R<RemainApplicationVO> apply(@PathVariable String uuid,
                                        @Valid @RequestBody RemainApplicationCreateDTO request) {
        return R.success(applicationQueryService.toView(
                applicationCommandService.apply(uuid, request)));
    }

    @PostMapping("/applications/{applicationUuid}/reverse")
    @RequirePermission(Permissions.REMAIN_ROLLBACK)
    public R<RemainApplicationVO> reverseApplication(@PathVariable String applicationUuid,
                                                     @Valid @RequestBody RemainApplicationReverseDTO request) {
        return R.success(applicationQueryService.toView(
                applicationReverseService.reverse(applicationUuid, request)));
    }

    @PostMapping("/{uuid}/rollback")
    @RequirePermission(Permissions.REMAIN_ROLLBACK)
    public R<RemainRegistrationVO> rollback(@PathVariable String uuid,
                                             @Valid @RequestBody RemainRollbackDTO request) {
        return R.success(registrationService.rollback(uuid, request));
    }

    @PostMapping("/sales")
    @RequirePermission(Permissions.REMAIN_SALE)
    public R<RemainSale> createSale(@Valid @RequestBody RemainSaleCreateDTO request) {
        return R.success(saleCommandService.create(request));
    }

    @GetMapping("/sales")
    @RequirePermission(Permissions.REMAIN_VIEW)
    public R<List<RemainSale>> listSales() {
        return R.success(saleQueryService.list());
    }

    @PostMapping("/sales/{saleUuid}/reverse")
    @RequirePermission(Permissions.REMAIN_SALE)
    public R<RemainSale> reverseSale(@PathVariable String saleUuid,
                                     @Valid @RequestBody RemainSaleReverseDTO request) {
        return R.success(saleReverseService.reverse(saleUuid, request));
    }
}
