# Phase 5 增强功能开发计划

生成时间：2026-06-24  
计划版本：Phase 4完成后增强迭代  
预计总工期：12-16天

---

## 一、开发背景

### 当前完成度
- Phase 1-4 核心功能已完成（98%）
- 主业务流程完整闭环（创建→打印→回录→出库→结算）
- 系统已达到上线标准

### 待补齐功能
1. **追加工序功能**：数据模型支持但无API端点
2. **全链路溯源查询**：质量追溯需求
3. **分批出库支持**：部分出库场景
4. **UI完整性修复**：打印模板精调、移动端适配
5. **错误降级完善**：全局Loading、自动重试

---

## 二、阶段1：核心功能增强（5-8天）

### 1.1 追加工序功能 ⏱️ 2-3天

#### 后端任务（1.5天）

**API设计**：
```
POST   /api/process-orders/{orderUuid}/steps       # 新增工序
PUT    /api/process-orders/steps/{stepUuid}        # 修改工序
DELETE /api/process-orders/steps/{stepUuid}        # 删除工序
```

**核心功能**：
- [ ] ProcessStepController 新增三个接口
- [ ] 工序顺序管理（step_order字段，自动递增）
- [ ] 主工艺唯一性约束校验（每个母卷只能有一个主工艺）
- [ ] 工序变更触发计费重算（调用calcProcessOrderFee）
- [ ] 单元测试（混合工艺场景覆盖）

**业务规则**：
1. 主工艺标识：`is_main_step=1`，每个原纸卷只能有一个主工艺
2. 追加工序：`is_main_step=0`，可多个
3. 删除工序时校验：主工艺不可删除（除非删除整个原纸）
4. 工序变更后自动标记`is_mix_process`

**数据库约束**：
```sql
-- 唯一索引：每个原纸卷只能有一个主工艺
CREATE UNIQUE INDEX uk_roll_main_step 
ON biz_process_step(original_roll_uuid, is_main_step) 
WHERE is_main_step = 1 AND is_deleted = 0;
```

#### 前端任务（0.5-1天）

**页面改造**：
- [ ] OrderDetailDrawer 工序Tab改造
- [ ] 新增工序按钮（状态=待下发时可用）
- [ ] 工序表单弹窗（工序类型、机台、参数、单价）
- [ ] 主工艺标记显示（Tag color="blue"）
- [ ] 删除工序二次确认（Popconfirm）
- [ ] 费用实时刷新（工序变更后重新查询详情）

**交互细节**：
1. 打印后禁止新增/删除工序（状态>=2）
2. 主工艺显示蓝色Tag，追加工序显示默认Tag
3. 删除主工艺时提示"主工艺不可删除"
4. 工序列表按step_order排序显示

---

### 1.2 全链路溯源查询 ⏱️ 2.5-3天

#### 后端任务（1.5天）

**API设计**：
```
GET /api/traceability/forward?rollUuid={uuid}      # 正向溯源：原纸→结算
GET /api/traceability/backward?settleUuid={uuid}   # 反向溯源：结算→原纸
GET /api/traceability/search?keyword={keyword}     # 通用搜索（卷号/单号）
```

**数据结构**：
```json
{
  "nodes": [
    {
      "type": "原纸",
      "uuid": "xxx",
      "name": "母卷001",
      "time": "2026-06-01 09:00:00",
      "status": "已完成"
    },
    {
      "type": "加工单",
      "uuid": "yyy",
      "name": "JG202606010001",
      "time": "2026-06-01 10:30:00",
      "status": "已完成"
    }
    // ... 成品、出库、结算节点
  ],
  "edges": [
    { "from": "xxx", "to": "yyy", "relation": "加工为" }
  ]
}
```

**核心功能**：
- [ ] TraceabilityController + TraceabilityService
- [ ] 正向溯源SQL（原纸→加工单→成品→出库→结算）
- [ ] 反向溯源SQL（结算→加工单→原纸）
- [ ] 多表关联查询优化（避免N+1查询）
- [ ] 数据不完整场景处理（未出库显示"待出库"）

**索引检查**：
```sql
-- 确保关联查询高效
CREATE INDEX idx_finish_roll_order ON biz_finish_roll(order_uuid);
CREATE INDEX idx_delivery_detail_finish ON biz_delivery_detail(finish_roll_uuid);
CREATE INDEX idx_settle_detail_delivery ON biz_settle_detail(delivery_order_uuid);
```

#### 前端任务（1-1.5天）

**页面设计**：
- [ ] 溯源查询页（`/traceability`）
- [ ] 搜索框（支持卷号/加工单号/结算单号/成品卷号）
- [ ] 时间轴展示（Ant Design Timeline组件）
- [ ] 节点详情弹窗（点击节点查看完整信息）
- [ ] 空状态提示（"未找到相关记录"）

**时间轴节点设计**：
```
[原纸] 母卷001
  ↓ 加工为
[加工单] JG202606010001
  ↓ 产出
[成品] A123456 (规格：1200mm×2000m)
  ↓ 出库
[出库单] CK202606050001
  ↓ 结算
[结算单] JS202606300001
```

**路由配置**：
```typescript
{ path: 'traceability', element: <TraceabilityPage /> }
```

**菜单配置**：
```typescript
{ key: '/traceability', icon: <ShareAltOutlined />, label: '全链路溯源' }
```

---

## 三、阶段2：分批出库（DDL变更）⏱️ 1.5-2天

### 3.1 DDL设计与审查（0.5天）

**数据库变更脚本**：
```sql
-- 迁移脚本：V1.1__add_remaining_weight.sql

-- 1. 新增剩余重量字段
ALTER TABLE biz_finish_roll 
ADD COLUMN remaining_weight DECIMAL(10,3) COMMENT '剩余库存重量kg' 
AFTER actual_weight;

-- 2. 数据初始化（已入库的成品，剩余库存=实际重量）
UPDATE biz_finish_roll 
SET remaining_weight = actual_weight 
WHERE finish_status = 2 
  AND remaining_weight IS NULL;

-- 3. 索引优化（出库查询频繁按剩余库存筛选）
CREATE INDEX idx_remaining_weight 
ON biz_finish_roll(remaining_weight, finish_status);

-- 4. 约束检查（剩余库存不能为负）
ALTER TABLE biz_finish_roll 
ADD CONSTRAINT chk_remaining_weight 
CHECK (remaining_weight >= 0);
```

**回滚脚本**：
```sql
-- V1.1__rollback.sql
ALTER TABLE biz_finish_roll DROP COLUMN remaining_weight;
DROP INDEX idx_remaining_weight ON biz_finish_roll;
```

**数据一致性校验**：
```sql
-- 验证脚本：迁移后remaining_weight应等于actual_weight
SELECT COUNT(*) AS inconsistent_count
FROM biz_finish_roll
WHERE finish_status = 2
  AND remaining_weight != actual_weight;
-- 期望结果：0
```

### 3.2 后端实现（0.5天）

**业务逻辑改造**：
- [ ] DeliveryService.confirmDelivery() 改造
- [ ] 库存扣减逻辑：`remaining_weight -= 出库重量`
- [ ] 库存不足校验：`remaining_weight < 出库重量 → 抛E003`
- [ ] 全部出库判定：`remaining_weight = 0 → finish_status = 3`
- [ ] 部分出库：`remaining_weight > 0 → finish_status 保持2`

**数据模型更新**：
```java
// FinishRoll.java
@TableField("remaining_weight")
private BigDecimal remainingWeight; // 剩余库存重量kg
```

**校验规则**：
```java
// 出库前校验
if (finishRoll.getRemainingWeight().compareTo(deliveryWeight) < 0) {
    throw new BizException("E003", "库存不足，剩余" + finishRoll.getRemainingWeight() + "kg");
}
```

### 3.3 前端实现（0.5-1天）

**出库单创建页改造**：
- [ ] 成品列表显示"剩余库存"列
- [ ] 出库重量输入框（默认=剩余库存，可修改）
- [ ] 库存不足前端预校验（输入>剩余时红色提示）
- [ ] 出库明细显示"本次出库 / 剩余库存"

**交互细节**：
```
成品卷号    规格         实际重量    剩余库存    出库重量
A123456    1200×2000    500.5kg     300.2kg    [___] kg
                                              ↑ 输入>300.2时红色边框提示
```

**状态显示优化**：
- 剩余库存=0：灰色Tag "已出空"
- 剩余库存>0：绿色Tag "库存充足"
- 剩余库存<实际重量50%：橙色Tag "库存偏低"

---

## 四、阶段3：UI完整性修复（5-7天）

### 4.1 打印模板精调（独立交付）⏱️ 1-2天

**任务清单**：
- [ ] 连接真实打印机测试（至少2种型号）
- [ ] CSS页边距调整（`@page { margin: ... }`）
- [ ] 缩放比例优化（A4/A5纸张适配）
- [ ] 母卷左合并布局验证（打印效果与预览一致）
- [ ] 二维码/条码尺寸优化（确保可扫描）
- [ ] 打印预览功能（Print Preview）

**测试用例**：
1. A4纸纵向打印（标准加工单）
2. A5纸横向打印（紧凑布局）
3. 混合工艺单据（红色大字标识清晰）
4. 多母卷合并打印（左侧母卷列表不换页）

**验收标准**：
- 打印内容不截断、不重叠
- 字体大小清晰可读（>=10pt）
- 卷号可扫描（如有条码）

### 4.2 移动端适配（重新设计交互）⏱️ 4-5天

#### 响应式断点设计
```css
/* 断点定义 */
@media (max-width: 768px)  { /* 手机 */ }
@media (max-width: 1024px) { /* 平板 */ }
@media (min-width: 1440px) { /* 大屏 */ }
```

#### 页面适配清单

**4.2.1 加工单列表（1天）**
- [ ] 桌面端：ProTable标准布局
- [ ] 移动端：Card卡片列表
  ```
  ┌─────────────────────────┐
  │ JG202606010001  [待下发]│
  │ 客户：XX纸业            │
  │ 制单日期：2026-06-01    │
  │ 总金额：¥12,345.67      │
  │ [详情] [打印] [下发]    │
  └─────────────────────────┘
  ```
- [ ] 搜索栏移动端固定顶部
- [ ] 筛选条件折叠（Drawer抽屉）

**4.2.2 加工单详情（1天）**
- [ ] 桌面端：Tab横向布局
- [ ] 移动端：Collapse折叠面板
  ```
  ▼ 基本信息
  ▼ 原纸明细 (3)
  ▼ 成品卷号 (12)
  ▼ 工序参数 (2)
  ```
- [ ] Drawer全屏显示（width="100%"）
- [ ] 操作按钮固定底部（Bottom Button Bar）

**4.2.3 回录页面（1天）**
- [ ] 桌面端：上下分层对照
- [ ] 移动端：单列滚动布局
  ```
  ┌─────────────┐
  │ [原纸1]     │
  │ 标称：...   │
  │ 实际：[输入]│
  ├─────────────┤
  │ [成品1]     │
  │ 预估：500kg │
  │ 实际：[输入]│
  └─────────────┘
  ```
- [ ] 实时偏差提示顶部固定（Sticky）
- [ ] 提交按钮固定底部

**4.2.4 出库/结算列表（0.5天）**
- [ ] 移动端卡片化（同加工单列表）
- [ ] 金额/数量字段突出显示（font-size: 18px）

**4.2.5 统计报表（0.5天）**
- [ ] 图表自适应宽度
- [ ] 表格横向滚动（overflow-x: auto）
- [ ] 筛选条件移动端Drawer展示

#### 触摸操作优化（1天）
- [ ] 列表项滑动删除（SwipeAction）
- [ ] 下拉刷新（PullToRefresh）
- [ ] 按钮最小尺寸44px×44px（符合触摸规范）
- [ ] 长按显示操作菜单（Context Menu）

#### 真机测试（设备兼容）
- [ ] iOS Safari 15+
- [ ] Android Chrome 100+
- [ ] 平板iPad横竖屏切换
- [ ] 性能测试（列表滚动流畅度）

---

## 五、阶段4：错误降级（可选）⏱️ 1天

**优先级最低**，核心功能已完成，此项为锦上添花。

### 5.1 全局Loading拦截器（0.3天）
```typescript
// src/api/request.ts
let loadingCount = 0;

instance.interceptors.request.use((config) => {
  loadingCount++;
  if (loadingCount === 1) {
    message.loading({ content: '加载中...', key: 'globalLoading', duration: 0 });
  }
  return config;
});

instance.interceptors.response.use(
  (resp) => {
    loadingCount = Math.max(0, loadingCount - 1);
    if (loadingCount === 0) {
      message.destroy('globalLoading');
    }
    return resp;
  },
  (error) => {
    loadingCount = Math.max(0, loadingCount - 1);
    if (loadingCount === 0) {
      message.destroy('globalLoading');
    }
    return Promise.reject(error);
  }
);
```

### 5.2 网络断开自动重试（0.4天）
```typescript
// 指数退避重试：1s、2s、4s
instance.interceptors.response.use(null, async (error) => {
  const config = error.config;
  if (!config || !config.retry) {
    config.retry = 0;
  }
  
  if (config.retry >= 3) {
    message.error('网络异常，请检查连接');
    return Promise.reject(error);
  }
  
  config.retry += 1;
  const delay = Math.pow(2, config.retry - 1) * 1000;
  
  await new Promise(resolve => setTimeout(resolve, delay));
  return instance.request(config);
});
```

### 5.3 Loading遮罩组件优化（0.3天）
- [ ] Spin全屏遮罩（长时间请求>3s显示）
- [ ] 取消按钮（可中断请求）
- [ ] 友好文案（"正在处理，请稍候..."）

---

## 六、执行顺序与里程碑

### 推荐执行顺序（按技术依赖和用户价值）

**Week 1（第1-5天）**：
- Day 1-2：追加工序（后端1.5天）
- Day 3：追加工序（前端0.5天） + 分批出库DDL（0.5天）
- Day 4-5：分批出库（后端0.5天 + 前端1天）

**Week 2（第6-10天）**：
- Day 6-7：全链路溯源（后端1.5天）
- Day 8-9：全链路溯源（前端1-1.5天）
- Day 10：打印模板精调（1天）

**Week 3（第11-16天）**：
- Day 11-15：移动端适配（4-5天）
- Day 16：错误降级（可选，1天）

### 里程碑定义

| 里程碑 | 判定标准 | 完成时间 |
|--------|----------|----------|
| M5.1   | 追加工序功能可用，计费自动更新 | Day 3 |
| M5.2   | 分批出库上线，DDL迁移完成 | Day 5 |
| M5.3   | 全链路溯源可查询，正反向路径完整 | Day 9 |
| M5.4   | 打印模板真实设备验证通过 | Day 10 |
| M5.5   | 移动端核心页面适配完成 | Day 15 |
| M5.6   | Phase 5 全部完成，系统增强版上线 | Day 16 |

---

## 七、风险控制

### 高风险点

| 风险 | 影响 | 对策 |
|------|------|------|
| 分批出库DDL迁移失败 | 数据不一致 | 先在测试环境迁移，验证脚本正确性 |
| 移动端适配工作量爆炸 | 延期 | 优先适配核心页面，次要页面降级（仅桌面端） |
| 打印模板真实设备不兼容 | 打印异常 | 准备多套模板，根据打印机型号动态切换 |
| 追加工序影响已有计费逻辑 | 金额错误 | 单元测试覆盖所有工序组合场景 |

### 质量保障

**代码审查**：
- 关键业务逻辑（计费、库存扣减）必须Code Review
- DDL脚本必须2人审查

**测试策略**：
- 单元测试：追加工序、分批出库核心逻辑
- 集成测试：全链路溯源多表关联查询
- 手工测试：打印模板真实设备验证
- 兼容性测试：移动端多设备真机测试

**回滚预案**：
- DDL迁移提供回滚脚本
- 功能开关（Feature Flag）控制新功能上线

---

## 八、验收标准

### 功能验收

**追加工序**：
- [ ] 可新增/修改/删除工序
- [ ] 主工艺唯一性约束生效
- [ ] 工序变更后计费自动更新
- [ ] 混合工艺自动标识

**全链路溯源**：
- [ ] 正向溯源：原纸→结算路径完整
- [ ] 反向溯源：结算→原纸路径完整
- [ ] 数据不完整场景友好提示
- [ ] 查询性能<2s

**分批出库**：
- [ ] 剩余库存字段正确显示
- [ ] 部分出库库存正确扣减
- [ ] 库存不足前端校验生效
- [ ] 全部出库状态自动更新

**打印模板**：
- [ ] A4/A5纸张打印正常
- [ ] 内容不截断、不重叠
- [ ] 条码可扫描（如有）

**移动端适配**：
- [ ] 核心页面在手机/平板可正常操作
- [ ] 触摸操作流畅无卡顿
- [ ] iOS/Android兼容

### 性能验收

- [ ] 列表查询<1s
- [ ] 溯源查询<2s
- [ ] 移动端首屏加载<3s
- [ ] 打印生成<2s

### 数据一致性验收

- [ ] 分批出库后库存余额正确
- [ ] 工序变更后总金额正确
- [ ] DDL迁移后数据无丢失

---

## 九、后续优化方向

Phase 5 完成后，系统已非常完善。后续可根据用户反馈优化：

1. **自动化测试补充**：Junit单测 + Selenium E2E
2. **性能监控接入**：APM工具（SkyWalking）
3. **数据报表增强**：更多维度统计分析
4. **权限体系细化**：角色权限、数据权限
5. **消息通知**：加工完成、出库到货消息推送

---

**文档维护人**：Claude Opus 4.7  
**最后更新**：2026-06-24  
**下一步行动**：开始追加工序功能开发（后端优先）
