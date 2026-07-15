package com.paper.mes.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageRequestBoundsTest {

    @Test
    void of_withInvalidValues_appliesSafeBounds() {
        Page<Object> page = PageRequestBounds.of(-10, Long.MAX_VALUE);

        assertEquals(1, page.getCurrent());
        assertEquals(100, page.getSize());
    }

    @Test
    void of_withNormalValues_preservesValues() {
        Page<Object> page = PageRequestBounds.of(3, 25);

        assertEquals(3, page.getCurrent());
        assertEquals(25, page.getSize());
    }
}
