package com.paper.mes.system.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.auth.mapper.SysUserMapper;
import com.paper.mes.auth.service.PasswordService;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.PageResult;
import com.paper.mes.common.ResultCode;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.system.user.dto.UserPasswordDTO;
import com.paper.mes.system.user.dto.UserQuery;
import com.paper.mes.system.user.dto.UserSaveDTO;
import com.paper.mes.system.user.dto.UserStatusDTO;
import com.paper.mes.system.user.dto.UserVO;
import com.paper.mes.system.user.service.UserManageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserManageServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserManageService {

    private static final int ENABLED = 1;
    private static final int DISABLED = 0;
    private static final List<String> ROLES = List.of("admin", "operator", "finance", "warehouse");

    private final PasswordService passwordService;
    private final OperationLogService operationLogService;

    public UserManageServiceImpl(PasswordService passwordService, OperationLogService operationLogService) {
        this.passwordService = passwordService;
        this.operationLogService = operationLogService;
    }

    @Override
    public PageResult<UserVO> pageUsers(UserQuery query) {
        requireAdmin();
        LambdaQueryWrapper<SysUser> wrapper = buildWrapper(query);
        Page<SysUser> page = page(Page.of(query.getCurrent(), query.getSize()), wrapper);
        PageResult<UserVO> result = new PageResult<>();
        result.setRecords(page.getRecords().stream().map(this::toVO).toList());
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }

    @Override
    public UserVO getByUuid(String uuid) {
        requireAdmin();
        return toVO(findUser(uuid));
    }

    @Override
    @Transactional
    public String create(UserSaveDTO dto) {
        requireAdmin();
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new BusinessException("新建用户必须设置初始密码");
        }
        ensureValidRole(dto.getRoleCode());
        ensureValidStatus(dto.getStatus());
        ensureUsernameUnique(dto.getUsername(), null);
        SysUser user = new SysUser();
        applySaveDto(user, dto);
        user.setPasswordHash(passwordService.encode(dto.getPassword()));
        save(user);
        operationLogService.record(OperationLogService.BIZ_TYPE_USER, user.getUuid(), user.getUsername(),
                OperationLogService.ACTION_USER_CREATE, null, "新增用户：" + user.getRealName());
        return user.getUuid();
    }

    @Override
    @Transactional
    public void update(String uuid, UserSaveDTO dto) {
        requireAdmin();
        ensureValidRole(dto.getRoleCode());
        ensureValidStatus(dto.getStatus());
        ensureUsernameUnique(dto.getUsername(), uuid);
        SysUser user = findUser(uuid);
        Integer version = user.getVersion();
        applySaveDto(user, dto);
        user.setUuid(uuid);
        user.setVersion(version);
        updateById(user);
        operationLogService.record(OperationLogService.BIZ_TYPE_USER, user.getUuid(), user.getUsername(),
                OperationLogService.ACTION_USER_UPDATE, null, "编辑用户：" + user.getRealName());
    }

    @Override
    @Transactional
    public void updateStatus(String uuid, UserStatusDTO dto) {
        requireAdmin();
        ensureValidStatus(dto.getStatus());
        SysUser user = findUser(uuid);
        if (isCurrentUser(uuid) && dto.getStatus() == DISABLED) {
            throw new BusinessException("不能停用当前登录账号");
        }
        user.setStatus(dto.getStatus());
        updateById(user);
        operationLogService.record(OperationLogService.BIZ_TYPE_USER, user.getUuid(), user.getUsername(),
                OperationLogService.ACTION_USER_STATUS, null, statusRemark(dto.getStatus()));
    }

    @Override
    @Transactional
    public void resetPassword(String uuid, UserPasswordDTO dto) {
        requireAdmin();
        SysUser user = findUser(uuid);
        user.setPasswordHash(passwordService.encode(dto.getPassword()));
        updateById(user);
        operationLogService.record(OperationLogService.BIZ_TYPE_USER, user.getUuid(), user.getUsername(),
                OperationLogService.ACTION_PASSWORD_RESET, null, "重置登录密码");
    }

    private LambdaQueryWrapper<SysUser> buildWrapper(UserQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysUser::getUsername, kw).or().like(SysUser::getRealName, kw));
        }
        if (StringUtils.hasText(query.getRoleCode())) {
            wrapper.eq(SysUser::getRoleCode, query.getRoleCode().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        return wrapper;
    }

    private void applySaveDto(SysUser user, UserSaveDTO dto) {
        user.setUsername(dto.getUsername().trim());
        user.setRealName(dto.getRealName().trim());
        user.setRoleCode(dto.getRoleCode().trim());
        user.setStatus(dto.getStatus() == null ? ENABLED : dto.getStatus());
        user.setRemark(dto.getRemark());
    }

    private SysUser findUser(String uuid) {
        SysUser user = getById(uuid);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private void ensureUsernameUnique(String username, String excludeUuid) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username.trim());
        if (StringUtils.hasText(excludeUuid)) {
            wrapper.ne(SysUser::getUuid, excludeUuid);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException("登录账号已存在：" + username);
        }
    }

    private void ensureValidRole(String roleCode) {
        if (!StringUtils.hasText(roleCode) || !ROLES.contains(roleCode.trim())) {
            throw new BusinessException("角色不正确");
        }
    }

    private void ensureValidStatus(Integer status) {
        if (status != null && status != ENABLED && status != DISABLED) {
            throw new BusinessException("状态不正确");
        }
    }

    private void requireAdmin() {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        if (user == null || !"admin".equals(user.getRoleCode())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有管理员可以维护用户权限");
        }
    }

    private boolean isCurrentUser(String uuid) {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        return user != null && uuid.equals(user.getUuid());
    }

    private String statusRemark(Integer status) {
        return status != null && status == ENABLED ? "启用账号" : "停用账号";
    }

    private UserVO toVO(SysUser user) {
        return UserVO.builder()
                .uuid(user.getUuid())
                .username(user.getUsername())
                .realName(user.getRealName())
                .roleCode(user.getRoleCode())
                .status(user.getStatus())
                .lastLoginTime(user.getLastLoginTime())
                .remark(user.getRemark())
                .createBy(user.getCreateBy())
                .updateBy(user.getUpdateBy())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }
}
