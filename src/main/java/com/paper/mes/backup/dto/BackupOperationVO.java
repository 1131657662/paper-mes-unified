package com.paper.mes.backup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BackupOperationVO {

    private boolean accepted;
    private String message;
}
