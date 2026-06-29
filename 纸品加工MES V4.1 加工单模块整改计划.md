# 纸品加工 MES V4.1 加工单模块整改计划

> 目的：对照根目录《卷筒纸加工管理系统 开发文档 V4.1》和《纸品加工MES系统 V4.1 开发执行计划》，修正当前加工单模块与原始需求的偏差。整改优先目标不是扩展新功能，而是先恢复真实可用的主业务链路：原纸 → 工艺 → 成品 → 打印 → 回录 → 计费。
>
> 2026-06-25 补充：复卷 5 类基础实现后，经业务复核确认当前模型仍存在结构性偏差。后续整改以根目录《加工配置与复卷模型重构方案.md》为准，前端目标从“能用”提升为“好用、人性化、方便”；必要时允许后端 DTO、字段和表结构大改。

## 一、当前总体结论

当前系统已经实现了不少页面、接口和后端基础能力，但加工单核心链路存在结构性偏差：

1. 新建加工单后，原纸、主工艺、工序、成品规格、成品卷号之间没有真实贯通。
2. 成品配置页存在大量前端占位交互，部分按钮提示成功但没有落库。
3. 复卷工艺文档要求 5 类，当前前端只提供 3 类，且计算为简化门幅分摊。
4. 后端已有部分计算能力，例如 `RewindWeightCalculator`，但未接入实际建单、配置、重算流程。
5. 工艺唯一来源按 V4.1 应为 `biz_process_step`，但当前建单主要依赖 `biz_original_roll.main_step_type`，并未自动生成主工序。
6. 回录闭合校验当前按整单聚合，不符合文档要求的单卷闭合。
7. 批量导入、现场定尺、回录阶段追加工序、多母卷合并复卷等关键场景尚未真正落地。

因此，当前系统适合作为半成品基础，但不适合直接按 V4.1 业务验收上线。

## 二、整改原则

1. 先打通真实主流程，再补复杂场景。
2. 前端不得假计算、假保存、假卷号；凡影响业务结果的数据必须由后端保存或返回。
3. 工艺判断统一回到 `biz_process_step`，`main_step_type` 只能作为录入辅助字段或冗余展示字段。
4. 成品卷号必须由后端全局生成，禁止前端拼接假号。
5. 重量、费用、状态、混合工艺判断以后端为准。
6. 每个阶段完成后必须做一条真实加工单的端到端验证。

## 三、P0：修复加工单主链路

目标：先让一张标准加工单真实可跑通，包括建单、主工序、成品号、打印、回录、计费。

### P0-1 创建加工单时自动生成主工序

#### 当前问题

新建页已经传入 `processMode` 和 `mainStepType`，但后端 `create()` 只插入 `biz_original_roll`，没有自动插入 `biz_process_step` 主工序。结果是用户在前端选择了主工艺，但后续计费、混合工艺判断、打印和详情中的工序数据不一定存在。

相关文件：

- `frontend/src/pages/processOrder/CreateOrderPage.tsx`
- `frontend/src/types/processOrder.ts`
- `src/main/java/com/paper/mes/processorder/dto/OriginalRollDTO.java`
- `src/main/java/com/paper/mes/processorder/service/impl/ProcessOrderServiceImpl.java`

#### 整改内容

1. 在 `ProcessOrderServiceImpl.create()` 中，每插入一条 `OriginalRoll` 后，依据 `mainStepType` 自动创建一条主工序。
2. 主工序字段规则：
   - `order_uuid = order.uuid`
   - `original_uuid = roll.uuid`
   - `step_sort = 1`
   - `step_type = roll.mainStepType`
   - `is_main = 1`
3. 如果 `processMode = 3` 直发，则不创建加工主工序。
4. 标准加工、现场定尺必须填写 `mainStepType`，否则后端拒绝创建。
5. 创建完成后更新 `is_mix_process`，判断是否存在锯纸和复卷混合。

#### 验收标准

创建一张两卷加工单：

- 原纸 1：标准加工 + 锯纸
- 原纸 2：标准加工 + 复卷

保存后详情页必须显示两条主工序，且后端 `biz_process_step` 中存在对应记录。

### P0-2 成品配置页改为真实保存

#### 当前问题

`ConfigFinishPage` 的保存逻辑只是 `setTimeout` 模拟成功，没有调用真实 API。锯纸、复卷组件中的成品规格、刀数、单价、备用号数量、复卷模式、预估重量都没有进入数据库。

相关文件：

- `frontend/src/pages/processOrder/ConfigFinishPage.tsx`
- `frontend/src/components/processOrder/FinishConfigPanel.tsx`
- `frontend/src/components/processOrder/SawingConfigForm.tsx`
- `frontend/src/components/processOrder/RewindingConfigForm.tsx`

#### 整改内容

1. 新增后端接口保存单卷成品配置，例如：

```http
POST /api/process-orders/{orderUuid}/rolls/{originalUuid}/finish-config
```

2. 请求体建议包含：

```json
{
  "processMode": 1,
  "mainStepType": 1,
  "finishSpecs": [
    {
      "finishWidth": 500,
      "finishDiameter": 30,
      "finishCoreDiameter": 3,
      "gramWeight": 120,
      "count": 2
    }
  ],
  "spareCount": 1,
  "stepConfig": {
    "knifeCount": 3,
    "processWeight": null,
    "unitPrice": 1.5,
    "lossWeight": 0
  },
  "rewindParams": []
}
```

3. 后端保存时应：
   - 更新原纸加工模式和主工艺。
   - 更新或创建主工序。
   - 删除或作废该原纸旧的未下发成品配置，避免重复生成。
   - 生成真实成品卷号并插入 `biz_finish_roll`。
   - 追加备用卷号。
   - 复卷时写入 `biz_process_param`。
   - 返回真实成品列表。
4. 前端保存按钮必须调用该接口，成功后刷新详情。
5. “保存全部并完成”必须逐卷校验是否保存成功，不能只提示成功。

#### 验收标准

在配置页为某卷原纸配置 3 个成品 + 1 个备用号后：

- 数据库中新增 4 条 `biz_finish_roll`。
- 卷号为后端生成的真实全局卷号。
- 详情页能看到这些成品。
- 打印页能使用这些真实卷号。

### P0-3 移除前端假卷号

#### 当前问题

前端存在硬编码假卷号：

- `A00${1000 + i}`
- `A00${2000 + i}`
- `A00${3000 + i}`
- `A00${4000 + i}`
- `A00${5000 + i}`

相关文件：

- `frontend/src/components/processOrder/SawingConfigForm.tsx`
- `frontend/src/components/processOrder/RewindingConfigForm.tsx`
- `frontend/src/components/processOrder/OnSiteConfigForm.tsx`

#### 整改内容

1. 删除前端拼接卷号逻辑。
2. 保存前只展示“预计生成 N 个正式号 / M 个备用号”。
3. 保存后展示后端返回的真实卷号。
4. 如果尚未保存，应明确显示“未生成”。

#### 验收标准

页面上不再出现任何前端拼接的假卷号。所有卷号均来自后端接口。

### P0-4 成品必须建立来源原纸关系

#### 当前问题

当前成品号生成接口按加工单批量生成，没有明确绑定到某条原纸。文档要求单件可追溯，尤其是单卷、多卷、合并复卷场景必须知道成品来源。

相关文件：

- `src/main/java/com/paper/mes/processorder/entity/FinishRoll.java`
- `sql/01_schema_v4.1.sql`
- `biz_finish_original_rel` 表

#### 整改内容

1. 单母卷加工场景，生成成品时至少要能追溯到 `original_uuid`。
2. 如果当前 `biz_finish_roll` 无 `original_uuid` 字段，则应优先使用 `biz_finish_original_rel` 建立关系。
3. 普通锯纸、普通复卷：每个成品关联一条原纸，`share_ratio = 100`。
4. 多母卷合并复卷：每个成品可关联多条原纸，并记录分摊比例和重量。
5. 回录闭合、打印、快照、溯源均应基于该关系。

#### 验收标准

任意成品卷号可以反查来源母卷；任意母卷可以查看其产出的所有成品。

### P0-5 打印必须基于真实配置

#### 当前问题

打印快照会读取 `finishRolls`，但如果成品配置没有真实保存，打印单就无法符合“母卷左合并 + 预打印成品卷号 + 母卷工艺标注”的要求。

相关文件：

- `src/main/java/com/paper/mes/processorder/service/impl/ProcessOrderServiceImpl.java`
- `frontend/src/pages/processOrder/PrintModal.tsx`

#### 整改内容

1. 首次打印前校验：所有非直发原纸必须已经有主工序和成品记录。
2. 打印快照中补充每卷主工艺、追加工序、成品来源关系。
3. 混合工艺单据打印时必须醒目标注。
4. 没有成品号时禁止打印，而不是打印空单。

#### 验收标准

一张包含锯纸 + 复卷 + 直发的混合单据，打印时必须能按母卷区块展示真实成品号和工艺。

## 四、P1：复卷 5 类完整落地

目标：实现文档 2.6 的 5 类复卷模式，并接入真实保存和计算流程。

### P1-1 前端补齐 5 类复卷模式

#### 当前问题

当前前端只有 3 类：

- 改门幅不变直径
- 改直径不变门幅
- 改门幅 + 改直径

缺少：

- 内外层分层不同规格
- 多母卷合并复卷

相关文件：

- `frontend/src/components/processOrder/RewindingConfigForm.tsx`

#### 整改内容

1. `REWIND_MODES` 补齐 5 类。
2. 模式 4 增加分层录入界面。
3. 模式 5 增加多母卷选择和分摊比例录入。
4. 不同模式展示不同参数表单。
5. 前端仅负责录入参数和展示后端结果，不负责最终计算。

#### 验收标准

复卷配置界面可选择 1~5 类模式，且每类都有对应参数录入区域。

### P1-2 保存 `biz_process_param`

#### 当前问题

数据库已有 `biz_process_param`，但当前业务保存流程没有真正使用。

相关文件：

- `sql/01_schema_v4.1.sql`
- `src/main/java/com/paper/mes/processorder/calc/RewindWeightCalculator.java`

#### 整改内容

1. 新增 `ProcessParam` 实体、Mapper、DTO、保存逻辑，若已存在则补齐当前缺失部分。
2. 单卷复卷配置保存时写入 `biz_process_param`。
3. 模式 1~3：按成品件或规格写参数。
4. 模式 4：按分层写参数。
5. 模式 5：按多母卷分摊比例写参数，并同步写 `biz_finish_original_rel`。

#### 验收标准

保存任意复卷模式后，数据库能查到对应 `param_mode` 和参数明细。

### P1-3 后端接入复卷重量计算

#### 当前问题

后端已有 `RewindWeightCalculator`，但未接入配置保存或回录重算流程。

相关文件：

- `src/main/java/com/paper/mes/processorder/calc/RewindWeightCalculator.java`
- `src/main/java/com/paper/mes/processorder/service/impl/ProcessOrderServiceImpl.java`

#### 整改内容

1. 成品配置保存时调用后端计算预估重量。
2. 回录录入实际原纸重量后，应支持一键重算成品预估重量。
3. 实称优先、面积分摊、修边分摊、尾差倒挤必须使用 `RewindWeightCalculator`。
4. 前端删除最终重量计算逻辑，只显示后端返回结果。

#### 验收标准

使用文档 2.6.1 两组样例计算，后端结果与期望值一致。

## 五、P2：回录改为单卷闭合

目标：将当前整单闭合改为符合 V4.1 的单卷闭合。

### P2-1 按原纸卷分别闭合校验

#### 当前问题

当前 `computeOrderClosure()` 是整单聚合闭合。这样会掩盖单卷异常。

相关文件：

- `src/main/java/com/paper/mes/processorder/service/impl/ProcessOrderServiceImpl.java`
- `src/main/java/com/paper/mes/processorder/calc/WeightCheckCalculator.java`

#### 整改内容

1. 根据成品来源关系，将成品、损耗、报废、修边归集到对应原纸。
2. 每条原纸单独调用 `WeightCheckCalculator.check()`。
3. 返回多个 `RollCheck`。
4. 任一单卷 BLOCK 时，整单提交应被拦截。
5. WARN 和 BLOCK 的原因、授权记录应关联具体原纸卷。

#### 验收标准

两卷原纸中一卷超差，另一卷正常时，系统必须指出具体超差母卷，而不是只给整单结果。

### P2-2 回录页按母卷分组展示

#### 当前问题

回录页有原纸区和成品区，但闭合结果不是按母卷分组展示，无法贴合纸质单据回录场景。

相关文件：

- `frontend/src/pages/processOrder/BackRecordDrawer.tsx`

#### 整改内容

1. 成品录入区按母卷分组。
2. 每个母卷显示标称参数、实际参数、成品合计、损耗、偏差率。
3. 支持一键复制标称值到实际值。
4. 支持尾差倒挤到当前母卷最后一件成品。
5. 支持按母卷显示 WARN/BLOCK。

#### 验收标准

回录时办公室人员能按纸质单据逐卷录入和校验，不需要自己人工对照整单数据。

## 六、P3：补齐建单效率和复杂业务场景

目标：补齐 V4.1 中影响真实使用效率和复杂场景的能力。

### P3-1 原纸批量导入真正落地

#### 当前问题

导入按钮只是提示待实现。

相关文件：

- `frontend/src/pages/processOrder/CreateOrderPage.tsx`

#### 整改内容

1. 前端支持 CSV/XLSX 解析或上传后端解析。
2. 模板字段包括：品名、克重、门幅、卷号、单件重量、直径、纸芯、长度、批次号、破损描述、备注。
3. 缺失必填字段、负数、格式错误要拦截。
4. 导入完成后支持批量设置加工模式和主工艺。
5. 失败行可导出错误清单。

#### 验收标准

使用根目录模板文件导入多条原纸后，能直接进入加工模式和成品配置步骤。

### P3-2 现场定尺流程落地

#### 当前问题

现场定尺当前只是前端输入预计件数，生成的是假卷号，未真正接入备用号作废和回录补规格。

相关文件：

- `frontend/src/components/processOrder/OnSiteConfigForm.tsx`
- `frontend/src/components/processOrder/SawingConfigForm.tsx`
- `frontend/src/components/processOrder/RewindingConfigForm.tsx`
- `src/main/java/com/paper/mes/processorder/service/impl/FinishRollServiceImpl.java`

#### 整改内容

1. 建单时只录入预计最大成品数。
2. 后端预生成真实卷号。
3. 打印单显示正式号和备用号。
4. 回录时补充实际成品规格和实际重量。
5. 未使用备用号批量作废封存。

#### 验收标准

现场定尺单从建单到回录能完成“预分配号 → 使用部分号 → 作废未使用号”。

### P3-3 回录阶段追加工序

#### 当前问题

当前追加工序更偏向待下发阶段维护，文档要求车间现场追加后在回录页面录入。

相关文件：

- `frontend/src/pages/processOrder/OrderDetailDrawer.tsx`
- `frontend/src/pages/processOrder/BackRecordDrawer.tsx`
- `src/main/java/com/paper/mes/processorder/service/impl/ProcessOrderServiceImpl.java`

#### 整改内容

1. 回录页增加工序 Tab 或母卷内工序编辑区。
2. 待回录状态允许新增追加工序，`is_main = 0`。
3. 主工序仍不允许删除。
4. 新增工序的刀数、吨位、损耗、单价参与计费和闭合。

#### 验收标准

车间手写追加锯纸工序后，办公室可在回录页录入，并正确计入费用和损耗。

### P3-4 破损图片上传完善

#### 当前问题

后端已有上传接口，但前端建单/回录场景没有完整体验。

相关文件：

- `src/main/java/com/paper/mes/processorder/controller/ProcessOrderController.java`
- `src/main/java/com/paper/mes/processorder/service/impl/ProcessOrderServiceImpl.java`
- `frontend/src/pages/processOrder/CreateOrderPage.tsx`
- `frontend/src/pages/processOrder/BackRecordDrawer.tsx`

#### 整改内容

1. 原纸明细支持上传多张破损图片。
2. 表格显示图片数量和缩略图入口。
3. 回录阶段允许补充加工后破损图片。
4. 图片路径存入 `damage_images` JSON。

#### 验收标准

任意原纸可查看建单和回录阶段上传的破损图片。

## 七、P4：出库、结算、溯源收口

目标：在加工单主链路稳定后，完善后续闭环。

### P4-1 直发三不约束强化

#### 当前状态

后端回录时能生成 `source_type=2` 直发成品记录，基本方向正确。

#### 整改内容

1. 前端说明改为“直发不进入加工入库流程，不拆分，不作为后续加工原卷”。
2. 后端禁止直发成品被拆分、调重拆分或作为新加工单原纸来源。
3. 出库选择时明确标识直发来源。

### P4-2 出库与结算按真实成品来源工作

#### 整改内容

1. 出库只从已完成、可出库成品中选择。
2. 结算按真实工序费用汇总锯纸费和复卷费。
3. 多工序费用独立取整后累加。
4. 对账单能展示原纸、成品、工序、出库、收款的完整链路。

### P4-3 快照和溯源增强

#### 整改内容

1. `snap_print` 应包含下发时原纸、工序、成品号、成品规格、预估重量。
2. `snap_finish` 应包含回录实际参数、实际成品重量、作废号、工序费用、闭合结果。
3. 快照对比按母卷和成品展示差异。
4. 溯源支持原纸到成品、成品到出库、结算到加工单反查。

## 八、建议实施顺序

建议按以下顺序整改，避免越改越乱：

1. P0-1：创建加工单自动生成主工序。
2. P0-2：成品配置页真实保存。
3. P0-3：移除前端假卷号。
4. P0-4：建立成品来源原纸关系。
5. P0-5：打印前校验真实配置。
6. P2-1：回录改为单卷闭合。
7. P1-1 ~ P1-3：复卷 5 类完整落地。
8. P3-1：批量导入。
9. P3-2：现场定尺。
10. P3-3：回录阶段追加工序。
11. P4：出库、结算、快照、溯源收口。

## 九、阶段性验收用例

### 用例 1：标准锯纸单

1. 新建加工单。
2. 添加 1 卷原纸，标准加工，主工艺锯纸。
3. 配置 3 个成品门幅，追加 1 个备用号。
4. 保存后生成真实成品卷号。
5. 打印。
6. 标记待回录。
7. 回录实际原纸重量和成品重量。
8. 单卷闭合通过。
9. 计费按刀数计算。

### 用例 2：标准复卷模式 1

1. 新建 1 卷原纸，主工艺复卷。
2. 选择复卷模式 1：改门幅不变直径。
3. 配置多个成品门幅。
4. 后端按门幅比例和修边规则计算预估重量。
5. 回录后按单卷闭合。

### 用例 3：混合工艺单

1. 同一加工单中：原纸 1 锯纸，原纸 2 复卷。
2. 系统自动标记混合工艺。
3. 打印单显示混合工艺标识。
4. 计费分别统计锯纸和复卷。

### 用例 4：直发单

1. 添加一卷原纸，选择不加工直发。
2. 不生成字母流水成品号。
3. 回录时生成 `source_type=2` 直发成品记录。
4. 出库时可选择该直发记录。
5. 该记录不可拆分，不可作为后续加工原卷。

### 用例 5：现场定尺

1. 建单时只输入预计最大成品数。
2. 系统预生成真实成品号和备用号。
3. 回录时补实际规格和实际重量。
4. 未使用备用号作废封存。

## 十、完成定义

整改完成后，应满足：

1. 加工单新建流程和 V4.1 文档主流程一致。
2. 页面上不存在假保存、假卷号、假计算。
3. 工艺以 `biz_process_step` 为唯一业务来源。
4. 复卷 5 类模式至少在数据结构和后端计算上完整支持。
5. 回录按单卷闭合，能定位具体异常母卷。
6. 打印、回录、计费均使用同一套真实成品和工序数据。
7. 一张混合工艺加工单可以端到端跑通。
