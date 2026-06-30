package com.paper.mes.system.user.controller;

import com.paper.mes.common.PageResult;
import com.paper.mes.common.R;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.system.user.dto.UserPasswordDTO;
import com.paper.mes.system.user.dto.UserQuery;
import com.paper.mes.system.user.dto.UserSaveDTO;
import com.paper.mes.system.user.dto.UserStatusDTO;
import com.paper.mes.system.user.dto.UserVO;
import com.paper.mes.system.user.service.UserManageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequirePermission(Permissions.USER_MANAGE)
@RequiredArgsConstructor
public class UserManageController {

    private final UserManageService userManageService;

    @GetMapping
    public R<PageResult<UserVO>> page(UserQuery query) {
        return R.success(userManageService.pageUsers(query));
    }

    @GetMapping("/{uuid}")
    public R<UserVO> detail(@PathVariable String uuid) {
        return R.success(userManageService.getByUuid(uuid));
    }

    @PostMapping
    public R<String> create(@Valid @RequestBody UserSaveDTO dto) {
        return R.success(userManageService.create(dto));
    }

    @PutMapping("/{uuid}")
    public R<Void> update(@PathVariable String uuid, @Valid @RequestBody UserSaveDTO dto) {
        userManageService.update(uuid, dto);
        return R.success();
    }

    @PutMapping("/{uuid}/status")
    public R<Void> updateStatus(@PathVariable String uuid, @Valid @RequestBody UserStatusDTO dto) {
        userManageService.updateStatus(uuid, dto);
        return R.success();
    }

    @PutMapping("/{uuid}/password")
    public R<Void> resetPassword(@PathVariable String uuid, @Valid @RequestBody UserPasswordDTO dto) {
        userManageService.resetPassword(uuid, dto);
        return R.success();
    }
}
