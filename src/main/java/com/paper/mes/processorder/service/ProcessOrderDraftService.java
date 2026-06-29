package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.DraftOrderBaseDTO;
import com.paper.mes.processorder.dto.DraftOrderVO;
import com.paper.mes.processorder.dto.DraftSummaryVO;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.OriginalRollImportPreviewVO;
import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanBatchSaveDTO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.ProcessOrderSubmitVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProcessOrderDraftService {

    List<DraftSummaryVO> listDrafts();

    DraftOrderVO getDraft(String orderUuid);

    String createDraft(DraftOrderBaseDTO dto);

    void saveBaseInfo(String orderUuid, DraftOrderBaseDTO dto);

    void saveDraftProgress(String orderUuid, Integer currentStep);

    List<String> replaceOriginalRolls(String orderUuid, List<OriginalRollDTO> rolls);

    void saveProcessConfig(String orderUuid, String rollUuid, FinishConfigSaveDTO dto);

    PlanPreviewVO previewProcessPlan(String orderUuid, String rollUuid, ProcessPlanDTO plan);

    PlanPreviewVO saveProcessPlan(String orderUuid, String rollUuid, ProcessPlanDTO plan);

    List<PlanPreviewVO> saveProcessPlanBatch(String orderUuid, ProcessPlanBatchSaveDTO dto);

    OriginalRollImportPreviewVO importPreview(MultipartFile file);

    ProcessOrderSubmitVO submit(String orderUuid);
}
