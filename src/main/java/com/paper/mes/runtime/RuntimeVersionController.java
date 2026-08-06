package com.paper.mes.runtime;

import com.paper.mes.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/runtime")
@RequiredArgsConstructor
public class RuntimeVersionController {

    private final RuntimeVersionService versionService;

    @GetMapping("/version")
    public R<RuntimeVersionVO> version() {
        return R.success(versionService.current());
    }
}
