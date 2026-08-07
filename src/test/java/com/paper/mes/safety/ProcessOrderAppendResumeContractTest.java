package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessOrderAppendResumeContractTest {

    @Test
    void activeSession_resumesBeforeStrictCommitVersionCheck() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/paper/mes/processorder/service/ProcessOrderAppendService.java"));
        String start = slice(source, "public ProcessOrderAppendVO start", "public ProcessOrderAppendVO get");
        String commit = slice(source, "public ProcessOrderAppendVO.CommitResult commit", "public void cancel");

        assertThat(start).contains("ProcessOrderAppendVersionPolicy.requireAppendableStatus");
        assertThat(start).doesNotContain("activeSession.getBaseOrderVersion()");
        assertThat(commit).contains("requireAppendable(order, request.getExpectedOrderVersion())");
        assertThat(commit).doesNotContain("session.getBaseOrderVersion(), request.getExpectedOrderVersion()");
    }

    private String slice(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from);
        return source.substring(from, to);
    }
}
