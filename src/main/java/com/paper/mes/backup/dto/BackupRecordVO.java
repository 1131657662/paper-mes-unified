package com.paper.mes.backup.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BackupRecordVO {

    private String id;
    private LocalDateTime createdAt;
    private long sizeBytes;
    private boolean databaseArchive;
    private boolean uploadIncluded;
    private boolean checksumAvailable;
    private String integrityStatus;
    private List<String> missingItems;
    private String verificationStatus;
    private LocalDateTime verifiedAt;
}
