package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.paper.mes.common.PageResult;
import com.paper.mes.processorder.dto.BackRecordDTO;
import com.paper.mes.processorder.dto.BackRecordResultVO;
import com.paper.mes.processorder.dto.FeeResultVO;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSaveVO;
import com.paper.mes.processorder.dto.FinishConfigBatchSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigBatchSaveVO;
import com.paper.mes.processorder.dto.FinishPreviewVO;
import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.dto.OriginalRollRemarkDTO;
import com.paper.mes.processorder.dto.PrintDTO;
import com.paper.mes.processorder.dto.PhysicalReprintDTO;
import com.paper.mes.processorder.dto.PrintViewVersion;
import com.paper.mes.processorder.dto.PrintResultVO;
import com.paper.mes.processorder.dto.ProcessOrderCreateDTO;
import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.dto.ProcessOrderPrintViewVO;
import com.paper.mes.processorder.dto.ProcessOrderQuery;
import com.paper.mes.processorder.dto.ProcessOrderRemarkDTO;
import com.paper.mes.processorder.dto.ProcessOrderVoidDTO;
import com.paper.mes.processorder.dto.ProcessStepDTO;
import com.paper.mes.processorder.dto.ProcessStepBatchDTO;
import com.paper.mes.processorder.dto.ProcessStepBatchResultVO;
import com.paper.mes.processorder.dto.ProcessStepPricingAdjustmentDTO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import com.paper.mes.processorder.dto.SnapshotDiffVO;
import com.paper.mes.processorder.entity.ProcessOrder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProcessOrderService extends IService<ProcessOrder> {

    PageResult<ProcessOrder> pageOrders(ProcessOrderQuery query);

    ProcessOrderDetailVO getDetail(String uuid);

    /** 读取下发冻结版本或完工实际版本的只读打印详情。 */
    ProcessOrderPrintViewVO getPrintView(String uuid, PrintViewVersion version);

    /** 导出加工单详情资料 Excel。 */
    String create(ProcessOrderCreateDTO dto);

    /** 向已有加工单追加一条原纸明细，返回新明细 uuid。 */
    String addRoll(String orderUuid, OriginalRollDTO dto);

    /** 轻量修改主单备注，不改动客户、日期、金额、状态等核心字段。 */
    void updateOrderRemark(String uuid, ProcessOrderRemarkDTO dto);

    /** 修改一条原纸明细。 */
    void updateRoll(String rollUuid, OriginalRollDTO dto);

    /** 轻量修改原纸明细备注类字段，不改动规格、重量、工艺等核心字段。 */
    void updateRollRemark(String rollUuid, OriginalRollRemarkDTO dto);

    /** 软删除一条原纸明细。 */
    void deleteRoll(String rollUuid);

    /** 保存单卷成品配置，生成正式成品号与备用号。 */
    FinishConfigSaveVO saveFinishConfig(String orderUuid, String rollUuid, FinishConfigSaveDTO dto);

    /** Persist multiple mother-roll configurations in one transaction. */
    FinishConfigBatchSaveVO saveFinishConfigBatch(String orderUuid, FinishConfigBatchSaveDTO dto);

    /** 根据复卷方案生成成品预览，不分配正式卷号。 */
    FinishPreviewVO previewRewindPlan(String orderUuid, String rollUuid, RewindPlanPreviewDTO dto);

    /** 加工单状态流转（状态机校验合法性，乐观锁更新）。 */
    void changeStatus(String uuid, Integer targetStatus, String reason);

    /** Internal settlement command; never exposed through the public status endpoint. */
    void markSettled(String uuid);

    /**
     * 完成车间加工并进入待回录。该命令只允许 PROCESSING -> TO_RECORD，
     * 不开放任意目标状态，避免绕过打印和回录业务校验。
     */
    void completeProcessing(String uuid, String reason);

    /** 深度回退草稿：用于已下发/已回录后需要更换母卷或重做方案。 */
    void rollbackToDraft(String uuid, String reason);

    /** 整单作废：仅草稿/待下发可操作，必须记录原因。 */
    void voidOrder(String uuid, ProcessOrderVoidDTO dto);

    /** 原纸单卷状态流转。 */
    void changeRollStatus(String rollUuid, Integer targetStatus);

    /** 确认物理打印完成并回写打印次数/时间；首次确认可无原因，补打必须留痕。 */
    PrintResultVO print(String uuid, PrintDTO dto);

    /** 对已下发后的历史版本执行物理补打审计，不改变加工单状态。 */
    PrintResultVO physicalReprint(String uuid, PhysicalReprintDTO dto);

    /** 下发加工单：生成不可变下发快照并流转到加工中，但不记录为已打印。 */
    PrintResultVO issue(String uuid);

    /**
     * 回录提交：写入原纸复称实际参数与成品实际重量，逐卷三级闭合校验，
     * 直发卷自动产出沿用母卷号的直发成品，作废未使用备用号，生成完成快照 snap_finish，
     * 状态 待回录(3) → 已完成(4)。>5% 超差需授权放行并写操作日志。
     */
    BackRecordResultVO backRecord(String uuid, BackRecordDTO dto);

    /**
     * 整单基础计费重算（P1-5）：逐工序算 step_amount（锯纸=刀数×单价/复卷=吨位×单价，取整），
     * 单卷 process_amount = Σ工序，整单汇总加工费/附加费/税额/总额并落库。
     * 单价取 step.unit_price，缺省回退客户 saw_price/rewind_price；
     * 复卷吨位取 step.process_weight，缺省取原纸 actual_weight/1000。幂等可重算。
     */
    FeeResultVO calcFee(String uuid);

    /** 核定已回录/已完成工序的特殊计价，供月结前确认最终应收。 */
    FeeResultVO adjustProcessStepPricing(String stepUuid, ProcessStepPricingAdjustmentDTO dto);

    /**
     * 双版本快照对比（P2-6）：snap_print 标称值 vs snap_finish 实际值，按 uuid 配对输出
     * 核心维度差异（原纸克重/门幅、成品预估vs实际重量）。任一快照缺失抛 E002。
     */
    SnapshotDiffVO snapshotDiff(String uuid);

    /**
     * 上传破损图片并绑定原纸（P2-4）：多图本地落盘，路径追加合并进该原纸 damage_images
     * （JSON 数组，保留原有图），返回合并后的完整 URL 列表。原纸不存在抛 E002。
     */
    List<String> uploadDamageImages(String rollUuid, MultipartFile[] files);

    /**
     * 新增工序（Phase 5.1）：仅待下发状态可操作，主工艺唯一性约束校验，
     * 自动分配 step_order，触发 is_mix_process 标识更新，自动重算计费。
     */
    void addProcessStep(String orderUuid, ProcessStepDTO dto);

    /** 在同一事务内向多卷母卷批量新增或覆盖附加工艺。 */
    ProcessStepBatchResultVO addProcessSteps(String orderUuid, ProcessStepBatchDTO dto);

    /**
     * 修改工序：草稿或待下发状态可操作，自动重算计费。
     */
    void updateProcessStep(String stepUuid, ProcessStepDTO dto);

    /**
     * 删除工序：草稿或待下发状态可操作，主工艺不可删除，自动重算计费。
     */
    void deleteProcessStep(String stepUuid);
}
