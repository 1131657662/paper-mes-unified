package com.paper.mes.common;

/**
 * 并发冲突断言（P3-1）。MyBatis-Plus 乐观锁更新命中 0 行时返回 false/0 而不抛异常，
 * 裸调用会把并发覆盖静默当成成功。所有依赖乐观锁的关键写操作经此断言，0 行即抛 E006。
 */
public final class ConcurrencyGuard {

    private ConcurrencyGuard() {
    }

    /** 断言 ServiceImpl.updateById(entity) 生效；false（0 行）即并发冲突。 */
    public static void requireUpdated(boolean updated) {
        if (!updated) {
            throw new BusinessException(ErrorCode.E006);
        }
    }

    /** 断言 Mapper.updateById(entity) 生效；影响行数 < 1 即并发冲突。 */
    public static void requireRowUpdated(int rows) {
        if (rows < 1) {
            throw new BusinessException(ErrorCode.E006);
        }
    }
}
