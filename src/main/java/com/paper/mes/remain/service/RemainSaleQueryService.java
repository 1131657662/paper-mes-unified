package com.paper.mes.remain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.remain.entity.RemainSale;
import com.paper.mes.remain.mapper.RemainSaleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RemainSaleQueryService {

    private final RemainSaleMapper saleMapper;

    public List<RemainSale> list() {
        return saleMapper.selectList(new LambdaQueryWrapper<RemainSale>()
                .orderByDesc(RemainSale::getProcessDate)
                .orderByDesc(RemainSale::getCreateTime));
    }
}
