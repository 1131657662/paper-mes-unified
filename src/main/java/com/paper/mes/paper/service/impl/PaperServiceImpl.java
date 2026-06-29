package com.paper.mes.paper.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.PageResult;
import com.paper.mes.paper.dto.PaperQuery;
import com.paper.mes.paper.dto.PaperSaveDTO;
import com.paper.mes.paper.entity.Paper;
import com.paper.mes.paper.mapper.PaperMapper;
import com.paper.mes.paper.service.PaperService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

    @Override
    public PageResult<Paper> pagePapers(PaperQuery query) {
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(Paper::getPaperCode, kw)
                    .or().like(Paper::getPaperName, kw));
        }
        wrapper.orderByDesc(Paper::getCreateTime);
        Page<Paper> page = page(Page.of(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.of(page);
    }

    @Override
    public Paper getByUuid(String uuid) {
        Paper paper = getById(uuid);
        if (paper == null) {
            throw new BusinessException("纸张不存在");
        }
        return paper;
    }

    @Override
    public String create(PaperSaveDTO dto) {
        ensureCodeUnique(dto.getPaperCode(), null);
        Paper paper = new Paper();
        BeanUtils.copyProperties(dto, paper);
        save(paper);
        return paper.getUuid();
    }

    @Override
    public void update(String uuid, PaperSaveDTO dto) {
        Paper existing = getByUuid(uuid);
        ensureCodeUnique(dto.getPaperCode(), uuid);
        Integer savedVersion = existing.getVersion();
        BeanUtils.copyProperties(dto, existing);
        existing.setUuid(uuid);
        existing.setVersion(savedVersion);
        updateById(existing);
    }

    @Override
    public void delete(String uuid) {
        getByUuid(uuid);
        removeById(uuid);
    }

    private void ensureCodeUnique(String paperCode, String excludeUuid) {
        if (!StringUtils.hasText(paperCode)) {
            return;
        }
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<Paper>()
                .eq(Paper::getPaperCode, paperCode);
        if (StringUtils.hasText(excludeUuid)) {
            wrapper.ne(Paper::getUuid, excludeUuid);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException("纸张编码已存在：" + paperCode);
        }
    }
}
