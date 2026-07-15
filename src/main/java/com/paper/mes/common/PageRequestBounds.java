package com.paper.mes.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public final class PageRequestBounds {

    public static final long MAX_SIZE = 100;

    private PageRequestBounds() {
    }

    public static <T> Page<T> of(long current, long size) {
        long safeCurrent = Math.max(1, current);
        long safeSize = Math.min(MAX_SIZE, Math.max(1, size));
        return Page.of(safeCurrent, safeSize);
    }
}
