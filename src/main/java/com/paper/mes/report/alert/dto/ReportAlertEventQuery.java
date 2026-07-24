package com.paper.mes.report.alert.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ReportAlertEventQuery {
    @Min(1)
    private Integer page = 1;
    @Min(1)
    @Max(100)
    private Integer size = 20;
    @Min(1)
    @Max(3)
    private Integer status;
    @Min(1)
    @Max(2)
    private Integer severity;
    @Size(max = 100)
    private String keyword;
    @Pattern(regexp = "^[0-9a-fA-F-]{32,36}$")
    private String focusUuid;

    public int pageNumber() {
        return page == null ? 1 : page;
    }

    public int pageSize() {
        return size == null ? 20 : size;
    }
}
