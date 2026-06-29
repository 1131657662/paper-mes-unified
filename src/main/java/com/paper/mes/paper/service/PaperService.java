package com.paper.mes.paper.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.paper.mes.common.PageResult;
import com.paper.mes.paper.dto.PaperQuery;
import com.paper.mes.paper.dto.PaperSaveDTO;
import com.paper.mes.paper.entity.Paper;

public interface PaperService extends IService<Paper> {

    PageResult<Paper> pagePapers(PaperQuery query);

    Paper getByUuid(String uuid);

    String create(PaperSaveDTO dto);

    void update(String uuid, PaperSaveDTO dto);

    void delete(String uuid);
}
