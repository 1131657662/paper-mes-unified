package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.paper.mes.processorder.dto.FinishRollBatchDTO;
import com.paper.mes.processorder.dto.SpareRollAppendDTO;
import com.paper.mes.processorder.entity.FinishRoll;

import java.util.List;

public interface FinishRollService extends IService<FinishRoll> {

    /** 成品卷状态流转（状态机校验合法性，乐观锁更新）。 */
    void changeFinishStatus(String uuid, Integer targetStatus);

    /** 批量生成正式成品卷号（is_spare=0），返回生成的卷号清单。 */
    List<String> batchGenerate(String orderUuid, FinishRollBatchDTO dto);

    /** 追加备用卷号（is_spare=1，顺延全局流水），返回生成的卷号清单。 */
    List<String> appendSpare(String orderUuid, SpareRollAppendDTO dto);

    /** 作废封存卷号（roll_no_status=3，永久不复用）。 */
    void voidRollNo(String finishUuid);

    /** 多选批量作废未使用的预生成卷号（roll_no_status=3）；整批校验，任一非法整体回滚。返回作废条数。 */
    int batchVoidRollNo(List<String> finishUuids);

    /** 全局查重：返回 true 表示卷号可用（未被占用）。excludeUuid 用于编辑时排除自身。 */
    boolean isRollNoAvailable(String rollNo, String excludeUuid);
}
