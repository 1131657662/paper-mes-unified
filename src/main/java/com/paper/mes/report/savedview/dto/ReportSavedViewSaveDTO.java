package com.paper.mes.report.savedview.dto;

import com.paper.mes.report.dto.ReportQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ReportSavedViewSaveDTO {
    private static final String REPORT_PATH_PATTERN = "^/reports/(overview|production|quality-loss|settlement|collection|inventory|delivery|explorer)$";
    private static final String DIMENSION_PATTERN = "^(month|customer|paper|process|machine|invoice|settleType|status)$";

    @NotBlank
    @Size(max = 100)
    private String viewName;
    @NotBlank
    @Pattern(regexp = REPORT_PATH_PATTERN)
    private String reportPath;
    @Valid
    @NotNull
    private ReportQuery reportQuery;
    @Pattern(regexp = DIMENSION_PATTERN)
    private String dimensionCode;
    @NotNull
    @Size(min = 1, max = 8)
    private List<@Pattern(regexp = "^[a-z][a-z0-9_]{1,63}$") String> metricCodes;
    @NotNull
    @Min(0)
    @Max(1)
    private Integer isDefault;
    @Min(1)
    private Integer version;
}
