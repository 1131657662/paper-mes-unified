package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackRecordReopenServiceTest {

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), OriginalRoll.class);
    }

    @Test
    void reopen_resetsReviewFieldsWithoutClearingRecordedValues() {
        OriginalRollMapper mapper = mock(OriginalRollMapper.class);
        when(mapper.update(isNull(), org.mockito.ArgumentMatchers.any())).thenReturn(2);
        BackRecordReopenService service = new BackRecordReopenService(mapper);

        service.reopen("order-1", "tester");

        ArgumentCaptor<LambdaUpdateWrapper<OriginalRoll>> wrapper = wrapperCaptor();
        verify(mapper).update(isNull(), wrapper.capture());
        String sqlSet = wrapper.getValue().getSqlSet();
        assertThat(sqlSet).contains("is_checked", "check_user", "check_time", "version = version + 1");
        assertThat(sqlSet).doesNotContain("actual_weight", "actual_width", "actual_gram_weight");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<LambdaUpdateWrapper<OriginalRoll>> wrapperCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
    }
}
