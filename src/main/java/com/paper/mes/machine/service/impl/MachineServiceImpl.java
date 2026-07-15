package com.paper.mes.machine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.PageResult;
import com.paper.mes.common.PageRequestBounds;
import com.paper.mes.machine.dto.MachineQuery;
import com.paper.mes.machine.dto.MachineSaveDTO;
import com.paper.mes.machine.entity.Machine;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.machine.service.MachineService;
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
public class MachineServiceImpl extends ServiceImpl<MachineMapper, Machine> implements MachineService {

    private static final int STATUS_ENABLED = 1;

    private final DocumentNoService documentNoService;

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
        Page<Machine> page = page(PageRequestBounds.of(query.getCurrent(), query.getSize()), wrapper);
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
    @Transactional(rollbackFor = Exception.class)
    public String create(MachineSaveDTO dto) {
        Machine machine = new Machine();
        BeanUtils.copyProperties(dto, machine);
        machine.setMachineCode(documentNoService.next(NoRuleBizType.MACHINE, LocalDate.now()));
        if (machine.getStatus() == null) {
            machine.setStatus(STATUS_ENABLED);
        }
        save(machine);
        return machine.getUuid();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String uuid, MachineSaveDTO dto) {
        Machine existing = getByUuid(uuid);
        Integer savedVersion = existing.getVersion();
        Integer keepStatus = existing.getStatus();
        String savedCode = existing.getMachineCode();
        BeanUtils.copyProperties(dto, existing);
        existing.setUuid(uuid);
        existing.setVersion(savedVersion);
        existing.setMachineCode(keepCodeOrGenerate(savedCode));
        if (existing.getStatus() == null) {
            existing.setStatus(keepStatus);
        }
        ConcurrencyGuard.requireUpdated(updateById(existing));
    }

    @Override
    public void delete(String uuid) {
        getByUuid(uuid);
        removeById(uuid);
    }

    private String keepCodeOrGenerate(String code) {
        return StringUtils.hasText(code) ? code : documentNoService.next(NoRuleBizType.MACHINE, LocalDate.now());
    }
}
