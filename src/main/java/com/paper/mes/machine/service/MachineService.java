package com.paper.mes.machine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.paper.mes.common.PageResult;
import com.paper.mes.machine.dto.MachineQuery;
import com.paper.mes.machine.dto.MachineSaveDTO;
import com.paper.mes.machine.dto.MachineVO;
import com.paper.mes.machine.entity.Machine;

public interface MachineService extends IService<Machine> {

    PageResult<MachineVO> pageMachines(MachineQuery query);

    Machine getByUuid(String uuid);

    MachineVO getProfile(String uuid);

    String create(MachineSaveDTO dto);

    void update(String uuid, MachineSaveDTO dto);

    void delete(String uuid);
}
