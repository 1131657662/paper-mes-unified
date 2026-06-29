package com.paper.mes.machine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.PageResult;
import com.paper.mes.machine.dto.MachineQuery;
import com.paper.mes.machine.dto.MachineSaveDTO;
import com.paper.mes.machine.entity.Machine;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.machine.service.MachineService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MachineServiceImpl extends ServiceImpl<MachineMapper, Machine> implements MachineService {

    private static final int STATUS_ENABLED = 1;

    @Override
    public PageResult<Machine> pageMachines(MachineQuery query) {
        LambdaQueryWrapper<Machine> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(Machine::getMachineCode, kw)
                    .or().like(Machine::getMachineName, kw));
        }
        if (query.getStatus() != null) {
            wrapper.eq(Machine::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Machine::getCreateTime);
        Page<Machine> page = page(Page.of(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.of(page);
    }

    @Override
    public Machine getByUuid(String uuid) {
        Machine machine = getById(uuid);
        if (machine == null) {
            throw new BusinessException("机台不存在");
        }
        return machine;
    }

    @Override
    public String create(MachineSaveDTO dto) {
        ensureCodeUnique(dto.getMachineCode(), null);
        Machine machine = new Machine();
        BeanUtils.copyProperties(dto, machine);
        if (machine.getStatus() == null) {
            machine.setStatus(STATUS_ENABLED);
        }
        save(machine);
        return machine.getUuid();
    }

    @Override
    public void update(String uuid, MachineSaveDTO dto) {
        Machine existing = getByUuid(uuid);
        ensureCodeUnique(dto.getMachineCode(), uuid);
        Integer savedVersion = existing.getVersion();
        Integer keepStatus = existing.getStatus();
        BeanUtils.copyProperties(dto, existing);
        existing.setUuid(uuid);
        existing.setVersion(savedVersion);
        if (existing.getStatus() == null) {
            existing.setStatus(keepStatus);
        }
        updateById(existing);
    }

    @Override
    public void delete(String uuid) {
        getByUuid(uuid);
        removeById(uuid);
    }

    private void ensureCodeUnique(String machineCode, String excludeUuid) {
        if (!StringUtils.hasText(machineCode)) {
            return;
        }
        LambdaQueryWrapper<Machine> wrapper = new LambdaQueryWrapper<Machine>()
                .eq(Machine::getMachineCode, machineCode);
        if (StringUtils.hasText(excludeUuid)) {
            wrapper.ne(Machine::getUuid, excludeUuid);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException("机台编码已存在：" + machineCode);
        }
    }
}
