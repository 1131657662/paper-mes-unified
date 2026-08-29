package com.paper.mes.processorder.service;

import com.paper.mes.processorder.entity.ProcessOrderIssueVersion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Formats compact audit metadata for large immutable issue snapshots. */
public final class ProcessOrderIssueSnapshotAudit {

    private ProcessOrderIssueSnapshotAudit() {
    }

    public static String summary(ProcessOrderIssueVersion version, String snapshotAfter) {
        return "下发版本 V" + version.getPreviousVersionNo() + " -> V" + version.getVersionNo()
                + "；变更前快照" + describe(version.getSnapshotBefore())
                + "；变更后快照" + describe(snapshotAfter)
                + requestSuffix(version.getRequestId());
    }

    private static String requestSuffix(String requestId) {
        return requestId == null || requestId.isBlank() ? "" : "；请求编号 " + requestId;
    }

    private static String describe(String snapshot) {
        if (snapshot == null) return "不存在";
        byte[] bytes = snapshot.getBytes(StandardCharsets.UTF_8);
        return bytes.length + "字节，SHA-256=" + sha256(bytes);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256不可用", exception);
        }
    }
}
