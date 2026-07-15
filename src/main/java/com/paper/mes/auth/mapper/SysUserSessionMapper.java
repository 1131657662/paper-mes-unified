package com.paper.mes.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.paper.mes.auth.entity.SysUserSession;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface SysUserSessionMapper extends BaseMapper<SysUserSession> {

    @Delete("DELETE FROM sys_user_session WHERE expire_time < #{cutoff}")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
