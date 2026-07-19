package com.paper.mes.settle.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.common.R;
import com.paper.mes.settle.dto.SettleCollectionReminderRequestDTO;
import com.paper.mes.settle.dto.SettleCollectionReminderVO;
import com.paper.mes.settle.dto.SettleCollectionSummaryVO;
import com.paper.mes.settle.dto.SettleQuery;
import com.paper.mes.settle.service.SettleCollectionReminderService;
import com.paper.mes.settle.service.SettleCollectionSummaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/settle-orders")
@RequiredArgsConstructor
public class SettleCollectionController {
    private final SettleCollectionReminderService reminderService;
    private final SettleCollectionSummaryService summaryService;

    @GetMapping("/collection-summary")
    @RequirePermission(Permissions.SETTLE_VIEW)
    public R<SettleCollectionSummaryVO> summary(@Valid SettleQuery query) {
        return R.success(summaryService.summarize(query));
    }

    @GetMapping("/{uuid}/collection-reminders")
    @RequirePermission(Permissions.SETTLE_VIEW)
    public R<List<SettleCollectionReminderVO>> reminders(@PathVariable String uuid) {
        return R.success(reminderService.list(uuid));
    }

    @PostMapping("/{uuid}/collection-reminders")
    @RequirePermission(Permissions.SETTLE_RECEIVE)
    public R<String> record(@PathVariable String uuid,
            @Valid @RequestBody SettleCollectionReminderRequestDTO dto) {
        return R.success(reminderService.record(uuid, dto));
    }
}
