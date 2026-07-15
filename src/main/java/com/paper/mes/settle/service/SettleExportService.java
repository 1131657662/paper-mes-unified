package com.paper.mes.settle.service;

import com.paper.mes.settle.dto.SettleDetailVO;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 * 结算单 Excel 导出装配。
 */
@Service
public class SettleExportService {

    public Workbook buildWorkbook(SettleDetailVO detail) {
        Workbook workbook = new XSSFWorkbook();
        SettleBillSheetWriter.write(workbook, detail);
        SettleReceiveSheetWriter.write(workbook, detail);
        SettleFeeSourceSheetWriter.write(workbook, detail);
        return workbook;
    }
}
