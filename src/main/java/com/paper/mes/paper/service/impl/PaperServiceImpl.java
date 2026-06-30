package com.paper.mes.paper.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.PageResult;
import com.paper.mes.paper.dto.PaperQuery;
import com.paper.mes.paper.dto.PaperSaveDTO;
import com.paper.mes.paper.entity.Paper;
import com.paper.mes.paper.mapper.PaperMapper;
import com.paper.mes.paper.service.PaperService;
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
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

    private final DocumentNoService documentNoService;

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
    @Transactional(rollbackFor = Exception.class)
    public String create(PaperSaveDTO dto) {
        Paper paper = new Paper();
        BeanUtils.copyProperties(dto, paper);
        paper.setPaperCode(documentNoService.next(NoRuleBizType.PAPER, LocalDate.now()));
        save(paper);
        return paper.getUuid();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String uuid, PaperSaveDTO dto) {
        Paper existing = getByUuid(uuid);
        Integer savedVersion = existing.getVersion();
        String savedCode = existing.getPaperCode();
        BeanUtils.copyProperties(dto, existing);
        existing.setUuid(uuid);
        existing.setVersion(savedVersion);
        existing.setPaperCode(keepCodeOrGenerate(savedCode));
        ConcurrencyGuard.requireUpdated(updateById(existing));
    }

    @Override
    public void delete(String uuid) {
        getByUuid(uuid);
        removeById(uuid);
    }

    private String keepCodeOrGenerate(String code) {
        return StringUtils.hasText(code) ? code : documentNoService.next(NoRuleBizType.PAPER, LocalDate.now());
    }
}
