package com.paper.mes.machine.controller;

import com.paper.mes.common.PageResult;
import com.paper.mes.common.R;
import com.paper.mes.machine.dto.MachineQuery;
import com.paper.mes.machine.dto.MachineSaveDTO;
import com.paper.mes.machine.entity.Machine;
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
    public R<PageResult<Machine>> page(MachineQuery query) {
        return R.success(machineService.pageMachines(query));
    }

    @GetMapping("/{uuid}")
    public R<Machine> detail(@PathVariable String uuid) {
        return R.success(machineService.getByUuid(uuid));
    }

    @PostMapping
    public R<String> create(@Valid @RequestBody MachineSaveDTO dto) {
        return R.success(machineService.create(dto));
    }

    @PutMapping("/{uuid}")
    public R<Void> update(@PathVariable String uuid, @Valid @RequestBody MachineSaveDTO dto) {
        machineService.update(uuid, dto);
        return R.success();
    }

    @DeleteMapping("/{uuid}")
    public R<Void> delete(@PathVariable String uuid) {
        machineService.delete(uuid);
        return R.success();
    }
}
