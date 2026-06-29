package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.paper.mes.common.PageResult;
import com.paper.mes.processorder.dto.BackRecordDTO;
import com.paper.mes.processorder.dto.BackRecordResultVO;
import com.paper.mes.processorder.dto.FeeResultVO;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSaveVO;
import com.paper.mes.processorder.dto.FinishPreviewVO;
import com.paper.mes.processorder.dto.OriginalRollDTO;
import com.paper.mes.processorder.dto.PrintDTO;
import com.paper.mes.processorder.dto.PrintResultVO;
import com.paper.mes.processorder.dto.ProcessOrderCreateDTO;
import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.dto.ProcessOrderQuery;
import com.paper.mes.processorder.dto.ProcessStepDTO;
import com.paper.mes.processorder.dto.RewindPlanPreviewDTO;
import com.paper.mes.processorder.dto.SnapshotDiffVO;
import com.paper.mes.processorder.entity.ProcessOrder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProcessOrderService extends IService<ProcessOrder> {

    PageResult<ProcessOrder> pageOrders(ProcessOrderQuery query);

    ProcessOrderDetailVO getDetail(String uuid);

    String create(ProcessOrderCreateDTO dto);

    /** 向已有加工单追加一条原纸明细，返回新明细 uuid。 */
    String addRoll(String orderUuid, OriginalRollDTO dto);

    /** 修改一条原纸明细。 */
    void updateRoll(String rollUuid, OriginalRollDTO dto);

    /** 软删除一条原纸明细。 */
    void deleteRoll(String rollUuid);

    /** 保存单卷成品配置，生成正式成品号与备用号。 */
    FinishConfigSaveVO saveFinishConfig(String orderUuid, String rollUuid, FinishConfigSaveDTO dto);

    /** 根据复卷方案生成成品预览，不分配正式卷号。 */
    FinishPreviewVO previewRewindPlan(String orderUuid, String rollUuid, RewindPlanPreviewDTO dto);

    /** 加工单状态流转（状态机校验合法性，乐观锁更新）。 */
    void changeStatus(String uuid, Integer targetStatus);

    /** 原纸单卷状态流转。 */
    void changeRollStatus(String rollUuid, Integer targetStatus);

    /**
     * 打印加工单：锁定原纸标称参数生成下发快照 snap_print，回写打印次数/时间，
     * 首打将状态由待下发流转为加工中。补打需带原因。
     */
    PrintResultVO print(String uuid, PrintDTO dto);

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

    /**
     * 修改工序（Phase 5.1）：仅待下发状态可操作，自动重算计费。
     */
    void updateProcessStep(String stepUuid, ProcessStepDTO dto);

    /**
     * 删除工序（Phase 5.1）：仅待下发状态可操作，主工艺不可删除，自动重算计费。
     */
    void deleteProcessStep(String stepUuid);
}
