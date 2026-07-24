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
import com.paper.mes.machine.dto.MachineCapabilitySaveDTO;
import com.paper.mes.machine.dto.MachineVO;
import com.paper.mes.machine.entity.Machine;
import com.paper.mes.machine.mapper.MachineMapper;
import com.paper.mes.machine.service.MachineCapabilityReader;
import com.paper.mes.machine.service.MachineCapabilityWriter;
import com.paper.mes.machine.service.MachineService;
import com.paper.mes.system.config.constant.NoRuleBizType;
import com.paper.mes.system.config.service.DocumentNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MachineServiceImpl extends ServiceImpl<MachineMapper, Machine> implements MachineService {

    private static final int STATUS_ENABLED = 1;
    private static final String RESOURCE_MACHINE = "MACHINE";

    private final DocumentNoService documentNoService;
    private final MachineCapabilityReader capabilityReader;
    private final MachineCapabilityWriter capabilityWriter;

    @Override
    public PageResult<MachineVO> pageMachines(MachineQuery query) {
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
        return capabilityReader.toPage(page);
    }

    @Override
    public Machine getByUuid(String uuid) {
        Machine machine = getById(uuid);
        if (machine == null) {
            throw new BusinessException("机台或工位不存在");
        }
        return machine;
    }

    @Override
    public MachineVO getProfile(String uuid) {
        return capabilityReader.toView(getByUuid(uuid));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(MachineSaveDTO dto) {
        List<MachineCapabilitySaveDTO> capabilities = capabilityWriter.normalize(
                dto.getCapabilities(), dto.getMachineType());
        Machine machine = new Machine();
        BeanUtils.copyProperties(dto, machine);
        machine.setMachineCode(documentNoService.next(NoRuleBizType.MACHINE, LocalDate.now()));
        machine.setStatus(machine.getStatus() == null ? STATUS_ENABLED : machine.getStatus());
        machine.setResourceKind(resourceKind(machine.getResourceKind()));
        machine.setMachineType(capabilityWriter.legacyType(capabilities));
        save(machine);
        capabilityWriter.replace(machine.getUuid(), machine.getStatus(), capabilities);
        return machine.getUuid();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String uuid, MachineSaveDTO dto) {
        Machine existing = getByUuid(uuid);
        List<MachineCapabilitySaveDTO> capabilities = capabilityWriter.normalizeForUpdate(
                uuid, dto.getCapabilities(), dto.getMachineType() == null
                        ? existing.getMachineType() : dto.getMachineType());
        Integer savedVersion = existing.getVersion();
        Integer keepStatus = existing.getStatus();
        String keepResourceKind = existing.getResourceKind();
        String savedCode = existing.getMachineCode();
        BeanUtils.copyProperties(dto, existing);
        existing.setUuid(uuid);
        existing.setVersion(savedVersion);
        existing.setMachineCode(keepCodeOrGenerate(savedCode));
        if (existing.getStatus() == null) {
            existing.setStatus(keepStatus);
        }
        existing.setResourceKind(resourceKind(existing.getResourceKind() == null
                ? keepResourceKind : existing.getResourceKind()));
        existing.setMachineType(capabilityWriter.legacyType(capabilities));
        ConcurrencyGuard.requireUpdated(updateById(existing));
        capabilityWriter.replace(uuid, existing.getStatus(), capabilities);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String uuid) {
        getByUuid(uuid);
        capabilityWriter.remove(uuid);
        removeById(uuid);
    }

    private String keepCodeOrGenerate(String code) {
        return StringUtils.hasText(code) ? code : documentNoService.next(NoRuleBizType.MACHINE, LocalDate.now());
    }

    private String resourceKind(String value) {
        return StringUtils.hasText(value) ? value : RESOURCE_MACHINE;
    }
}
