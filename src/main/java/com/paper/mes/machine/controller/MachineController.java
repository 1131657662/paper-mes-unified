package com.paper.mes.machine.controller;

import com.paper.mes.common.PageResult;
import com.paper.mes.common.R;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.machine.dto.MachineQuery;
import com.paper.mes.machine.dto.MachineSaveDTO;
import com.paper.mes.machine.dto.MachineVO;
import com.paper.mes.machine.service.MachineService;
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
@RequestMapping("/api/machines")
@RequiredArgsConstructor
public class MachineController {

    private final MachineService machineService;

    @GetMapping
    @RequirePermission(Permissions.BASE_VIEW)
    public R<PageResult<MachineVO>> page(MachineQuery query) {
        return R.success(machineService.pageMachines(query));
    }

    @GetMapping("/{uuid}")
    @RequirePermission(Permissions.BASE_VIEW)
    public R<MachineVO> detail(@PathVariable String uuid) {
        return R.success(machineService.getProfile(uuid));
    }

    @PostMapping
    @RequirePermission(Permissions.BASE_MANAGE)
    public R<String> create(@Valid @RequestBody MachineSaveDTO dto) {
        return R.success(machineService.create(dto));
    }

    @PutMapping("/{uuid}")
    @RequirePermission(Permissions.BASE_MANAGE)
    public R<Void> update(@PathVariable String uuid, @Valid @RequestBody MachineSaveDTO dto) {
        machineService.update(uuid, dto);
        return R.success();
    }

    @DeleteMapping("/{uuid}")
    @RequirePermission(Permissions.BASE_MANAGE)
    public R<Void> delete(@PathVariable String uuid) {
        machineService.delete(uuid);
        return R.success();
    }
}
