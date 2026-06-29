package com.paper.mes.processorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.processorder.dto.FinishRollBatchDTO;
import com.paper.mes.processorder.dto.SpareRollAppendDTO;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.service.FinishRollService;
import com.paper.mes.processorder.statemachine.FinishRollNoGenerator;
import com.paper.mes.processorder.statemachine.FinishStatus;
import com.paper.mes.processorder.statemachine.StateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinishRollServiceImpl extends ServiceImpl<FinishRollMapper, FinishRoll>
        implements FinishRollService {

    private static final int ROLL_NO_PRE = 1;   // 预生成
    private static final int ROLL_NO_VOID = 3;  // 作废封存
    private static final int IS_SPARE_NO = 0;
    private static final int IS_SPARE_YES = 1;
    private static final int SOURCE_PROCESS = 1; // 加工产出
    private static final int FINISH_STATUS_PENDING_IN = 1;
    // 预生成/备用号在回录前尚无品名规格，但 DDL 三列 NOT NULL，回录(P1-4)时覆盖。
    private static final String PLACEHOLDER_PAPER_NAME = "待定";
    private static final int PLACEHOLDER_GRAM_WEIGHT = 0;
    private static final int PLACEHOLDER_FINISH_WIDTH = 0;
    /** 仅字母流水卷号（A-Y + 6位数字）参与最大值计算，排除直发母卷号等异形值。 */
    private static final String LETTER_SEQ_REGEXP = "finish_roll_no REGEXP '^[A-Y][0-9]{6}$'";
    /** 唯一索引冲突时的重试次数，应对并发同时取到同一最大值。 */
    private static final int ALLOC_RETRY = 5;

    private final ProcessOrderMapper processOrderMapper;

    @Override
    public void changeFinishStatus(String uuid, Integer targetStatus) {
        FinishRoll finishRoll = getById(uuid);
        if (finishRoll == null) {
            throw new BusinessException("成品不存在");
        }
        FinishStatus from = FinishStatus.of(finishRoll.getFinishStatus());
        FinishStatus to = FinishStatus.of(targetStatus);
        StateMachine.assertTransition(from, to);

        // TODO P1-6：出库(2→3)在此校验库存扣减；报废(1→4)在此校验回录上下文。

        finishRoll.setFinishStatus(to.getCode());
        ConcurrencyGuard.requireUpdated(updateById(finishRoll));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> batchGenerate(String orderUuid, FinishRollBatchDTO dto) {
        ProcessOrder order = requireOrder(orderUuid);
        List<String> result = new ArrayList<>(dto.getCount());
        int rowSort = nextRowSort(orderUuid);
        for (int i = 0; i < dto.getCount(); i++) {
            FinishRoll roll = newRoll(order, rowSort++, IS_SPARE_NO);
            roll.setPaperName(dto.getPaperName() != null ? dto.getPaperName() : PLACEHOLDER_PAPER_NAME);
            roll.setCustomerPaperName(dto.getCustomerPaperName());
            roll.setGramWeight(dto.getGramWeight() != null ? dto.getGramWeight() : PLACEHOLDER_GRAM_WEIGHT);
            roll.setFinishWidth(dto.getFinishWidth() != null ? dto.getFinishWidth() : PLACEHOLDER_FINISH_WIDTH);
            roll.setFinishDiameter(dto.getFinishDiameter());
            roll.setFinishCoreDiameter(dto.getFinishCoreDiameter());
            roll.setWarehouseUuid(dto.getWarehouseUuid());
            roll.setRemark(dto.getRemark());
            result.add(allocAndInsert(roll));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> appendSpare(String orderUuid, SpareRollAppendDTO dto) {
        ProcessOrder order = requireOrder(orderUuid);
        List<String> result = new ArrayList<>(dto.getCount());
        int rowSort = nextRowSort(orderUuid);
        for (int i = 0; i < dto.getCount(); i++) {
            FinishRoll roll = newRoll(order, rowSort++, IS_SPARE_YES);
            result.add(allocAndInsert(roll));
        }
        return result;
    }

    @Override
    public void voidRollNo(String finishUuid) {
        FinishRoll roll = getById(finishUuid);
        if (roll == null) {
            throw new BusinessException("成品卷号不存在");
        }
        if (ROLL_NO_VOID == roll.getRollNoStatus()) {
            throw new BusinessException("该卷号已作废，无需重复操作");
        }
        // 已使用(2)的卷号已绑定实物成品，作废会丢失溯源；仅允许作废预生成(1)的未用号。
        if (ROLL_NO_PRE != roll.getRollNoStatus()) {
            throw new BusinessException("仅未使用的预生成卷号可作废封存");
        }
        roll.setRollNoStatus(ROLL_NO_VOID);
        ConcurrencyGuard.requireUpdated(updateById(roll));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchVoidRollNo(List<String> finishUuids) {
        List<FinishRoll> rolls = listByIds(finishUuids);
        if (rolls.size() != finishUuids.size()) {
            // 数量不符说明有 uuid 查不到，整批拒绝（与单个作废一致：不存在即报错）。
            throw new BusinessException("部分卷号不存在，批量作废已取消");
        }
        for (FinishRoll roll : rolls) {
            // 复用单个 voidRollNo 同款校验：已作废拒绝、非预生成态拒绝；任一非法整批回滚。
            if (ROLL_NO_VOID == roll.getRollNoStatus()) {
                throw new BusinessException("卷号 " + roll.getFinishRollNo() + " 已作废，批量作废已取消");
            }
            if (ROLL_NO_PRE != roll.getRollNoStatus()) {
                throw new BusinessException("卷号 " + roll.getFinishRollNo() + " 非未使用预生成号，批量作废已取消");
            }
            roll.setRollNoStatus(ROLL_NO_VOID);
        }
        // 逐条更新并断言乐观锁生效：updateBatchById 部分失败仍可能整体返回 true，
        // 改为逐条 requireUpdated，任一并发冲突在事务内整批回滚（P3-1）。
        for (FinishRoll roll : rolls) {
            ConcurrencyGuard.requireUpdated(updateById(roll));
        }
        return rolls.size();
    }

    @Override
    public boolean isRollNoAvailable(String rollNo, String excludeUuid) {
        if (rollNo == null || rollNo.isBlank()) {
            return false;
        }
        LambdaQueryWrapper<FinishRoll> wrapper = new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getFinishRollNo, rollNo.trim());
        if (excludeUuid != null && !excludeUuid.isBlank()) {
            wrapper.ne(FinishRoll::getUuid, excludeUuid);
        }
        return count(wrapper) == 0;
    }

    /**
     * 分配全局下一卷号并插入。并发下多线程可能取到同一最大值，
     * 唯一索引 uk_finish_roll_no 会拦截重复，捕获后重算重试。
     */
    private String allocAndInsert(FinishRoll roll) {
        for (int attempt = 0; attempt < ALLOC_RETRY; attempt++) {
            String rollNo = nextRollNo();
            roll.setUuid(null);
            roll.setFinishRollNo(rollNo);
            try {
                save(roll);
                return rollNo;
            } catch (DuplicateKeyException e) {
                // 卷号被并发抢占，重算下一个再试。
            }
        }
        throw new BusinessException("卷号分配冲突，请重试");
    }

    /** 全局下一字母流水卷号：取现有最大字母流水 +1，无则从 A000001 起。 */
    private String nextRollNo() {
        FinishRoll max = getOne(new LambdaQueryWrapper<FinishRoll>()
                .apply(LETTER_SEQ_REGEXP)
                .orderByDesc(FinishRoll::getFinishRollNo)
                .last("LIMIT 1"), false);
        if (max == null || max.getFinishRollNo() == null) {
            return FinishRollNoGenerator.encode(1);
        }
        return FinishRollNoGenerator.next(max.getFinishRollNo());
    }

    private FinishRoll newRoll(ProcessOrder order, int rowSort, int isSpare) {
        FinishRoll roll = new FinishRoll();
        roll.setOrderUuid(order.getUuid());
        roll.setRowSort(rowSort);
        roll.setRollNoStatus(ROLL_NO_PRE);
        roll.setIsSpare(isSpare);
        roll.setSourceType(SOURCE_PROCESS);
        roll.setFinishStatus(FINISH_STATUS_PENDING_IN);
        // DDL 三列 NOT NULL 占位，回录覆盖；batchGenerate 用模板值再行赋值。
        roll.setPaperName(PLACEHOLDER_PAPER_NAME);
        roll.setGramWeight(PLACEHOLDER_GRAM_WEIGHT);
        roll.setFinishWidth(PLACEHOLDER_FINISH_WIDTH);
        return roll;
    }

    /** 取该加工单成品明细当前最大 row_sort + 1。 */
    private int nextRowSort(String orderUuid) {
        FinishRoll top = getOne(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, orderUuid)
                .orderByDesc(FinishRoll::getRowSort)
                .last("LIMIT 1"), false);
        if (top == null || top.getRowSort() == null) {
            return 1;
        }
        return top.getRowSort() + 1;
    }

    private ProcessOrder requireOrder(String orderUuid) {
        ProcessOrder order = processOrderMapper.selectById(orderUuid);
        if (order == null) {
            throw new BusinessException("加工单不存在");
        }
        return order;
    }
}
