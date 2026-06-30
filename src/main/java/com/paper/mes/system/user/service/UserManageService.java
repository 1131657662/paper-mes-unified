package com.paper.mes.system.user.service;

import com.paper.mes.common.PageResult;
import com.paper.mes.system.user.dto.UserPasswordDTO;
import com.paper.mes.system.user.dto.UserQuery;
import com.paper.mes.system.user.dto.UserSaveDTO;
import com.paper.mes.system.user.dto.UserStatusDTO;
import com.paper.mes.system.user.dto.UserVO;

public interface UserManageService {

    PageResult<UserVO> pageUsers(UserQuery query);

    UserVO getByUuid(String uuid);

    String create(UserSaveDTO dto);

    void update(String uuid, UserSaveDTO dto);

    void updateStatus(String uuid, UserStatusDTO dto);

    void resetPassword(String uuid, UserPasswordDTO dto);
}
