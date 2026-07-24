package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.dto.ProcessCatalogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcessCatalogService {

    private final ProcessCatalogRepository repository;

    public List<ProcessCatalogVO> listActive() {
        return repository.findActive();
    }

    public ProcessCatalogVO requireActive(Integer stepType) {
        if (stepType == null) {
            throw new BusinessException(ErrorCode.E003, "工序类型不能为空");
        }
        return listActive().stream()
                .filter(entry -> entry.stepType() == stepType)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.E003, "工序类型未启用或不存在"));
    }
}
