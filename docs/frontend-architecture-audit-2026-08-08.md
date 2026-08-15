# MES 前端架构优化审计报告

> 审计日期：2026-08-08
> 审计范围：`D:\paper-mes-unified\frontend`，仅做只读诊断；未修改应用源码、依赖、配置或锁文件。
> 证据来源：源码与依赖树、Vite 生产构建、Vitest/Playwright/oxlint、已登录本地页面的 DOM/控制台/可见状态。GitHub 数据为审计当日 API 快照。

## 一、项目概览

- 项目名称：`paper-mes-unified`，页面品牌为“纸品智造 MES”。
- 技术栈：React `19.2.8` + Ant Design `5.29.3` + Pro Components `2.8.10` + React Router `8.3.0` + Vite `8.1.0` + TypeScript `6.0.2`。
- 状态与数据：TanStack Query `5.101.1`、Zustand `5.0.14`、Axios `1.18.1`、query-key-factory `1.3.4`。
- 可视化：ECharts `6.1.0`，已经采用 `echarts/core` 按组件注册，并对趋势图做近视口懒加载，这是正确方向。
- 代码规模：`src` 共 1,336 个文件，其中 529 个 TSX、704 个 TS、99 个 CSS；TypeScript 源码占比接近 100%，未发现生产代码中的显式 `any`。
- 编译约束：`strict`、`noUncheckedIndexedAccess`、`noUnusedLocals`、`noUnusedParameters` 均已开启。
- 依赖安全：`npm audit --omit=dev` 为 0 个已知漏洞；生产依赖树 207 项，总依赖树 308 项。
- 质量基线：`npm run build`、`npm run lint` 均通过；Vitest 222 个文件、713 个测试通过；E2E 18 个通过、33 个因缺少 E2E 凭据而跳过。

### 当前性能基线

| 指标 | 结果 | 解释 |
|---|---:|---|
| `dist/assets` | 282 个资源，4.58 MiB 原始体积 | 231 JS、50 CSS；资源粒度偏碎 |
| JS Chunk 分布 | 231 个；114 个小于 2 KiB；中位数 2 KiB | HTTP/2 可缓解，但路由切换仍有解析和调度成本 |
| 初始 HTML 直连资源 | 7 个，732.6 KiB 原始 / 233.4 KiB gzip | 包含入口、React、React Query、样式等 |
| 入口 `index-*.js` | 281.9 KiB 原始 / 91.2 KiB gzip | 内含 103 个唯一 `/api/...` 路径，明显超出认证启动职责 |
| 最大共享 Chunk | `useTableColumnsState-*.js` 487.0 KiB / 150.4 KiB gzip | 名称具有误导性；实际还导入 Table、DatePicker、Select 等共享运行时代码 |
| 图表运行时 | `lineChartRuntime-*.js` 516.1 KiB / 173.8 KiB gzip | 已懒加载，不属于首屏阻塞项，但应设预算 |
| Dashboard 就绪 | 约 837 ms | 本地开发环境、已登录暖会话；不是标准 Web Vital |
| 加工单列表就绪 | 约 1,489 ms | 同上，包含本地 API 响应和 50 行渲染 |
| Dashboard DOM | 约 862-920 节点 | 1366×768，无横向溢出、无控制台错误 |
| 加工单列表 DOM | 2,792 节点、51 行、2 个 table、141 个 button | 无横向溢出、无控制台错误；密集交互面已形成渲染压力 |

`FCP/LCP/INP/TTI` 本次未作为有效基线：登录态页面、本地开发服务器和当前浏览器接口不足以生成可复现的生产 Web Vitals。后续应在生产构建、固定数据和固定网络条件下用 Lighthouse CI 或 RUM 建立基线。

### 分维度健康度

| 维度 | 当前评分 | 主要判断 |
|---|---:|---|
| 打包与加载 | 62/100 | 页面级懒加载良好，但全局 Query Key 导致入口依赖泄漏，Chunk 过碎 |
| 运行时渲染 | 68/100 | 表格分页与固定布局完善，但无 Table 虚拟化，多个 columns 引用不稳定 |
| 状态与数据流 | 76/100 | TanStack Query 覆盖广，仍有 ProTable 直连 API、档案页 `useEffect` 拉数和认证双源状态 |
| Ant Design 使用 | 84/100 | ConfigProvider、主题 Token、错误态、分页、列宽持久化较成熟；虚拟表格和表单定位可补齐 |
| 无障碍与 UX | 68/100 | 主导航和多数按钮命名较好；存在低对比度、路由焦点、标题语义和表单错误定位缺口 |
| 代码健康度 | 82/100 | 严格 TS、无显式 any、测试强；少数超大组件/API/类型文件形成维护热点 |

## 二、问题诊断与优化方案清单（按优先级排序）

### [P0-紧急] 1. 切断认证启动对全业务 Query Registry 的静态依赖

- **问题定位**：`src/queries/index.ts:1`、`src/queries/index.ts:24`、`src/features/auth/hooks/useCurrentUser.ts:2`、`src/features/delivery/queries/deliveryKeys.ts:9`。
- **现状描述**：认证守卫只需要 `/api/auth/me`，但 `useCurrentUser` 导入了全局 `queries`；该文件静态合并 21 个领域的 key，而每个 key 又持有 service/queryFn。结果是认证启动把出库、结算、报表、系统配置等 API 模块带进入口。生产入口中可检出 103 个唯一 API 路径，页面级 `lazy()` 的收益被部分抵消。
- **方案 A（推荐，自实现）**：认证启动直接导入 `authKeys`；其余懒加载路由的 hooks 直接导入本领域 key，或至少拆成 `bootstrapQueries` 与路由本地 registry。保留 query-key-factory，不换库。

修改前：

```tsx
import { queries } from '../../../queries'

return useQuery({ ...queries.auth.currentUser, enabled })
```

修改后：

```tsx
import { authKeys } from '../queries/authKeys'

return useQuery({ ...authKeys.currentUser, enabled, retry: false })
```

- **方案 B（库对比）**：继续使用 [TanStack Query](https://github.com/TanStack/query)（50,085 Star，13.3 KiB gzip）而非迁移 [SWR](https://github.com/vercel/swr)（32,451 Star，5.4 KiB gzip）。SWR 更轻，但迁移 73 个 query 文件和 88 个 mutation 文件无法抵消改造风险；问题根因是导入边界，不是缓存库本身。
- **预期效果**：入口 `index` 从 91.2 KiB gzip 降至目标 45-60 KiB；初始 gzip 从 233.4 KiB 降至 175-195 KiB；本地 Dashboard 就绪时间目标下降 10%-20%。
- **风险**：全局失效逻辑需要改为导入对应领域 `_def`；必须增加“登录页/仪表盘入口不得包含非启动 API 字符串”的 bundle 回归检查。
- **工作量预估**：1.5-2.5 人天。

### [P1-重要] 2. 稳定 Table columns，并在高密度列表启用原生虚拟化

- **问题定位**：`src/pages/customer/CustomerList.tsx:58`、`src/components/useResizableTableColumns.tsx:44`、`src/pages/processOrder/ProcessOrderList.tsx:48`、`src/pages/processOrder/ProcessOrderListTable.tsx:58`。
- **现状描述**：Customer/Paper/Warehouse/User 等列表在每次渲染时重建 columns；`useResizableTableColumns` 的 `useMemo` 依赖 `columns`，因此上游引用变化会让列映射、表头回调和 ProTable 配置重复生成。加工单页一次渲染 50 行、141 个按钮和 2,792 个 DOM 节点，项目内没有任何 `virtual` 配置。
- **方案 A（推荐）**：先稳定 columns，再对固定行高、50 行以上的列表试点 Ant Design 自带 `virtual`。ProTable 类型继承 `TableProps`，无需新增运行时依赖；但虚拟表要求 `scroll.x/y` 为数字，当前 `y: '100%'` 必须改为容器实测高度。

修改前：

```tsx
const columns = buildProcessOrderColumns(commands.columnOptions)
<ProTable scroll={{ x: resizable.scrollX, y: '100%' }} />
```

修改后（示意）：

```tsx
const columns = useMemo(
  () => buildProcessOrderColumns(stableColumnOptions),
  [stableColumnOptions],
)

<ProTable
  columns={columns}
  virtual={pageSize >= 50}
  scroll={{ x: resizable.scrollX, y: tableBodyHeight }}
/>
```

- **方案 B（候选库）**：[TanStack Virtual](https://github.com/TanStack/virtual)（7,049 Star，7.2 KiB gzip）支持 headless、动态尺寸和复杂布局；[react-window](https://github.com/bvaughn/react-window)（17,202 Star，4.4 KiB gzip）更轻、API 简洁，但与 AntD table 语义、固定列和可变行高的整合成本更高。当前优先级应为 AntD 原生（0 KiB）> TanStack Virtual > react-window。
- **扩展选项**：若未来出现 1,000+ 行客户端编辑、冻结区域、复制粘贴需求，再评估 [AG Grid](https://github.com/ag-grid/ag-grid)（15,527 Star，约 222.9 KiB gzip，部分能力商业许可）或 [React Data Grid](https://github.com/Comcast/react-data-grid)（7,666 Star，15.3 KiB gzip，当前 npm 仍为 beta）。现阶段不应换表格体系。
- **预期效果**：虚拟化页 DOM 节点目标降至 900-1,300、行操作按钮降至约 40-60；筛选/状态切换 React commit p95 目标低于 100 ms。列稳定化本身预计减少 20%-40% 的无效列转换，需用 React Profiler 验证。
- **风险**：虚拟行与可变高度、展开行、固定列、列拖拽要逐页回归；50 行以内收益可能小于复杂度，因此必须设置启用阈值。
- **工作量预估**：2.5-4 人天。

### [P1-重要] 3. 将剩余服务器状态统一到 TanStack Query

- **问题定位**：`src/pages/processOrder/ProcessOrderListTable.tsx:54`、`src/pages/customer/CustomerDetailPage.tsx:28`、`src/pages/customer/CustomerFormPage.tsx:28`、`src/pages/paper/PaperDetailPage.tsx:21`、`src/pages/machine/MachineFormPage.tsx:26`、`src/pages/warehouse/WarehouseFormPage.tsx:25`。
- **现状描述**：主流程已经大量使用 Query，但加工单 ProTable 仍直接调用 API；8 个基础档案详情/编辑页在 `useEffect` 中请求并维护 `loading/data`。网络失败后详情页会落入“档案不存在”，把传输错误与 404 混为一谈；详情到编辑、前进后退也无法复用 30 秒缓存。
- **方案 A（推荐）**：为 customer/paper/machine/warehouse 建立领域 key 和单一职责 hooks；ProTable 可继续使用其 request UI，但 request 内用 `queryClient.fetchQuery`，或改成受控 `dataSource/loading`。

```tsx
export const customerKeys = createQueryKeys('customer', {
  detail: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => getCustomer(uuid),
  }),
})

export function useCustomer(uuid?: string) {
  return useQuery({
    ...customerKeys.detail(uuid ?? ''),
    enabled: Boolean(uuid),
  })
}
```

- **方案 B（库对比）**：TanStack Query 已具备请求去重、失效、缓存与 DevTools；SWR 体积更小但 mutation/invalidation 能力和现有集成不占优。建议增加 `@tanstack/react-query-devtools` 仅用于开发环境，而不是引入第二套服务端状态库。
- **预期效果**：30 秒内详情/编辑往返重复请求可降至 0；同 key 并发请求合并；失败页可区分 404、网络错误并提供重试。典型档案维护流程请求数预计下降 30%-60%。
- **风险**：ProTable 自身 reload 语义与 Query invalidation 需统一，否则可能出现双重刷新；迁移应按领域逐个完成。
- **工作量预估**：3-5 人天。

### [P1-重要] 4. 消除认证用户/权限的双源状态

- **问题定位**：`src/router/AuthGuard.tsx:9`、`src/router/AuthGuard.tsx:20`、`src/stores/authStore.ts:24`、`src/stores/authStore.ts:43`、`src/stores/authStore.ts:38`。
- **现状描述**：`/api/auth/me` 的返回数据只用于判定会话是否有效，未同步到 Zustand；`syncCurrentUser` 没有调用点。用户和权限被持久化到 localStorage，因此管理员变更权限后，前端按钮可能继续显示旧权限，直到重新登录。后端授权仍应是唯一安全边界，本项属于 UI/行为一致性问题，不应被描述为已确认的越权漏洞。
- **短期修复示意**：在认证恢复成功后把服务端用户同步到 store；长期把 `currentUser` 只保留在 Query/Context，Zustand 仅保留纯客户端 UI 状态。

```tsx
const { data: currentUser, ...session } = useCurrentUser(Boolean(cachedUser))
const { syncCurrentUser } = useAuthActions()

useEffect(() => {
  if (currentUser) syncCurrentUser(currentUser)
}, [currentUser, syncCurrentUser])
```

- **库对比**：[Zustand](https://github.com/pmndrs/zustand)（58,533 Star，0.5 KiB gzip）适合当前小型客户端状态；[Redux Toolkit](https://github.com/reduxjs/redux-toolkit)（11,224 Star，13.3 KiB gzip）在复杂事件审计和大型团队约束上更强，但本项目没有迁移收益。保留 Zustand，减少其职责。
- **预期效果**：权限增删在下一次 `/me` 校验后反映到 UI；减少“按钮可见但请求 403”或“已授权但按钮仍隐藏”的工单。
- **风险**：要明确会话刷新频率、离线策略和 store 迁移版本；禁止把 token/密码写入 localStorage。
- **工作量预估**：1-2 人天。

### [P1-重要] 5. 建立 WCAG 2.2 AA 与路由焦点基线

- **问题定位**：`src/styles/mes-theme.css:226`、`src/styles/app-shell.css:205`、`src/styles/app-shell.css:212`、`src/styles/app-shell.css:399`、`src/pages/dashboard/DashboardPage.css:766`、`src/layout/BasicLayout.tsx:48`、`src/pages/processOrder/ProcessOrderListHeader.tsx:34`、`src/pages/login/LoginFormPanel.tsx:63`。
- **现状描述**：`#8a98a8`、`#86909c`、`#98a2b3`、`#94a3b8` 在白底上的对比度约为 2.56-3.24:1，低于普通文字 4.5:1。路由切换只滚动到顶部，不把焦点移动到主区域/标题；加工单 Card 标题不是语义标题。登录页浏览器快照出现 1 个无可访问名称按钮，疑似 Password 可见性切换，需在真实读屏器中复核。长表单未配置 `scrollToFirstError`。
- **方案 A（推荐）**：把次要文字统一到现有主题 `#526579` 或经设计确认的 AA Token；主区域增加 `tabIndex={-1}` 并在路由切换后 focus；Card title 使用 `h1`；长 Form 使用 `scrollToFirstError={{ focus: true }}`。

```tsx
useEffect(() => {
  contentRef.current?.scrollTo({ top: 0, left: 0 })
  contentRef.current?.focus({ preventScroll: true })
}, [location.pathname, location.search])

<Content ref={contentRef} tabIndex={-1} aria-label={routeTitle}>...</Content>
```

```css
/* before */ color: #8a98a8;
/* after  */ color: #526579; /* white 上满足 AA，仍需自动化复核 */
```

- **方案 B（候选工具）**：[axe-core](https://github.com/dequelabs/axe-core)（7,377 Star）配合 `@axe-core/playwright`（46.1 KiB npm 解包）适合复用现有 Playwright 登录流程；[Pa11y](https://github.com/pa11y/pa11y)（4,485 Star）更适合独立 CLI/多 URL 扫描，但登录态编排和 AntD 动态层支持较弱。推荐 axe。
- **预期效果**：普通文字对比度 >= 4.5:1；键盘路由切换有可预测焦点；CI 中 serious/critical axe violation 目标为 0；表单首次错误可自动定位。
- **风险**：自动扫描只能发现约 30%-50% 的无障碍问题，仍需键盘、NVDA/读屏器人工抽查。
- **工作量预估**：2-3 人天。

### [P1-重要] 6. 拆分超大业务组件与领域文件

- **问题定位**：`src/components/processOrder/RewindingConfigForm.tsx:192`（组件延续至 624 行）、`src/layout/PageTabs.tsx:20`（331 行）、`src/api/processOrder.ts:1`（714 行、约 70 个导出函数）、`src/types/processOrder.ts:1`（1,154 行、约 103 个声明）、`src/pages/customer/CustomerList.tsx:27`（220 行）。
- **现状描述**：大部分 feature 已按 hooks/components/services 切分，但复卷配置仍在单组件内同时承担 DTO 转换、段/排布编辑、预览请求、派生统计和渲染。加工单 API/类型文件也已成为“领域总入口”，任何变化都会扩大理解和回归范围。
- **方案 A（推荐）**：按业务能力拆分，而不是按行数机械拆分。

```text
features/processOrderRewind/
  components/RewindSegmentList.tsx
  components/RewindPreviewPanel.tsx
  hooks/useRewindPlan.ts
  model/rewindMappers.ts
  model/rewindValidation.ts
```

- **方案 B（工具辅助）**：[Knip](https://github.com/webpro-nl/knip)（11,912 Star，持续维护）用于发现拆分后的未使用文件/导出/依赖；[depcheck](https://github.com/depcheck/depcheck)（4,930 Star）已归档，不建议新接入。组件职责检查仍要靠评审和测试，工具不能自动决定领域边界。
- **预期效果**：核心组件目标控制在 150-200 行，单个业务函数目标 30 行左右；复卷改动测试准备成本下降，冲突面减少。此项主要改善交付稳定性，不承诺直接提升首屏。
- **风险**：复卷算法具有强耦合，拆分前先补 model/hook 单测；不要为只使用一次的表达式制造通用 utils。
- **工作量预估**：4-6 人天。

### [P2-建议] 7. 建立 bundle 可视化与性能预算，不盲目手调 Chunk

- **问题定位**：`vite.config.ts:13`、`vite.config.ts:16`、`src/components/charts/lineChartRuntime.ts:1`、`src/features/dashboard/components/DashboardTrend.tsx:8`。
- **现状描述**：目前仅按 React、React Query、Pro Components 做 `manualChunks`，没有产物分析和预算。231 个 JS 中 114 个小于 2 KiB；同时有 10 个超过 100 KiB 的 Chunk。最大共享 Chunk 的文件名不能代表实际内容，直接根据名称拆包会误判。ECharts 已正确使用 core、SVG renderer、动态导入和近视口加载，不建议为了体积立即换图表库。
- **方案 A（推荐）**：接入 `rollup-plugin-visualizer`，记录 gzip/brotli；CI 对初始 gzip、代表路由新增请求数和最大异步 Chunk 设置预算。

```ts
import { visualizer } from 'rollup-plugin-visualizer'

plugins: [
  react(),
  visualizer({ filename: 'artifacts/bundle.html', gzipSize: true, brotliSize: true }),
]
```

- **方案 B（候选工具）**：[rollup-plugin-visualizer](https://github.com/btd/rollup-plugin-visualizer)（2,410 Star）与 Vite/Rollup 最直接；[Bundle Stats](https://github.com/relative-ci/bundle-stats)（671 Star）更适合 PR 差异和长期趋势。性能门禁可选 [Lighthouse CI](https://github.com/GoogleChrome/lighthouse-ci)（7,038 Star）或 [Sitespeed.io](https://github.com/sitespeedio/sitespeed.io)（5,009 Star）；前者更轻、CI 集成简单，后者更适合多地点/长周期监控。
- **图表选型**：ECharts 67,003 Star、全包约 359.3 KiB gzip；Recharts 27,478 Star、约 144.1 KiB gzip。当前实际 ECharts 异步 Chunk 为 173.8 KiB gzip且只在近视口加载，迁移会损失能力并带来回归；建议保留并将目标预算设为 <=150 KiB gzip。
- **预期效果**：每个 PR 可见体积变化；初始 gzip 超过 200 KiB、单个异步 gzip 超过 150 KiB 时阻断或告警；减少“优化后反而把懒路由拉进入口”的回归。
- **工作量预估**：1-2 人天。

### [P2-建议] 8. 收敛重复依赖与生产环境 shim

- **问题定位**：`package.json:17`、`index.html:12`、`vite.config.ts:26`、`vite.config.ts:30`、`vite.config.ts:36`。
- **现状描述**：依赖树存在 `@ant-design/icons` 6.2.5（直接）与 5.6.1（AntD/Pro 传递）两套；Zustand 5.0.14（直接）与 4.5.7（XYFlow 传递）两套。后者体积很小，前者值得用 visualizer 验证。生产 HTML 仍注入 `window.process.env.NODE_ENV = 'development'` 的兼容 shim，Vite config 也默认 development；这不等于已证明 React 使用开发构建，但会使依赖的运行时环境判断不可信。`allowedHosts: true` 同时用于 dev/preview，配合 ngrok 暴露时范围过宽。
- **方案 A（推荐）**：先用产物图确认重复成本；在 Pro Components 支持范围内统一 icons 主版本。确认第三方不再需要 Node `process` 后，删除 HTML shim，只定义精确常量；`allowedHosts` 改为 localhost 和实际隧道域名 allowlist。

```ts
export default defineConfig(({ mode }) => ({
  define: {
    'process.env.NODE_ENV': JSON.stringify(mode === 'production' ? 'production' : 'development'),
  },
  server: { allowedHosts: ['localhost', '.ngrok-free.app'] },
}))
```

- **方案 B（工具）**：Knip 负责未使用依赖和导出；`npm ls` 负责多版本树；不建议自动强制 override 不兼容的 icons/Zustand 主版本。
- **预期效果**：避免运行时错误模式误判；视 visualizer 结果，依赖去重预计节省 5-20 KiB gzip；降低开发隧道的 Host 暴露面。
- **工作量预估**：0.5-1.5 人天。

### [P2-建议] 9. 用 OpenAPI 生成收敛 API/类型边界

- **问题定位**：`src/api/request.ts:72`、`src/api/request.ts:74`、`src/api/processOrder.ts:1`、`src/types/processOrder.ts:1`。
- **现状描述**：编译期类型非常严格，但 Axios 响应在边界使用断言解包，运行时不校验；加工单 API 与类型规模已大，手工维护容易产生字段漂移。当前审计未发现显式 `any`，因此重点不是“补 TS”，而是让后端契约成为可生成、可比较的来源。
- **方案 A（推荐）**：若 Spring Boot 已提供稳定 OpenAPI，先选择只读查询或基础档案做生成试点，生成 DTO、Axios client 和 Query options；手写 request 继续承载 request-id、业务错误映射和 401 处理。

```ts
// orval.config.ts（示意）
export default defineConfig({
  paperMes: {
    input: '../docs/openapi.json',
    output: {
      target: 'src/api/generated/paperMes.ts',
      client: 'react-query',
      httpClient: 'axios',
      override: { mutator: { path: 'src/api/request.ts', name: 'request' } },
    },
  },
})
```

- **方案 B（候选库）**：[Orval](https://github.com/orval-labs/orval)（6,337 Star）对 Axios + TanStack Query 开箱支持更成熟；[Kubb](https://github.com/kubb-labs/kubb)（1,770 Star）插件化和 Zod 生成更灵活。当前栈优先 Orval；若必须生成运行时 schema，再评估 Kubb。
- **预期效果**：API 字段漂移在 CI 生成 diff 阶段暴露；手写重复 DTO/请求函数目标减少 40%-70%；新增接口交付时间下降。
- **风险**：若 OpenAPI 注解不完整，生成代码只会把错误自动化；禁止一次性替换 70 个加工单 API，必须小域试点。
- **工作量预估**：试点 2-3 人天，全面迁移另计。

## 三、外部生态资源推荐汇总表

> Star 和最近活跃度来自 2026-08-08 GitHub API；体积为 Bundlephobia 完整包估算或 npm 解包体积，只用于相对比较，实际以本项目 tree-shaking 产物为准。

| 场景 | 推荐方案 | 替代方案 | 结论 |
|---|---|---|---|
| 服务端状态 | TanStack Query：50,085 Star，13.3 KiB gzip，2026-08-03 有推送 | SWR：32,451 Star，5.4 KiB gzip，2026-08-07 有推送 | 保留 TanStack；修复导入边界和覆盖缺口 |
| 表格虚拟化 | AntD Table 原生：99,000 Star，新增 0 KiB | TanStack Virtual：7,049 Star / 7.2 KiB；react-window：17,202 Star / 4.4 KiB | 先原生，复杂动态行再 TanStack Virtual |
| 超大工业网格 | AG Grid：15,527 Star / 222.9 KiB gzip | React Data Grid：7,666 Star / 15.3 KiB gzip、npm beta | 仅在 1,000+ 行编辑需求成立时评估 |
| 客户端状态 | Zustand：58,533 Star / 0.5 KiB gzip | Redux Toolkit：11,224 Star / 13.3 KiB gzip | 保留 Zustand，缩小到纯客户端状态 |
| A11y CI | axe-core：7,377 Star，2026-08-07 有推送 | Pa11y：4,485 Star，2026-08-03 有推送 | axe 更适合现有 Playwright 登录流程 |
| Bundle 分析 | rollup-plugin-visualizer：2,410 Star | Bundle Stats：671 Star | 前者先落地，后者适合长期 PR 趋势 |
| 性能门禁 | Lighthouse CI：7,038 Star | Sitespeed.io：5,009 Star | 单应用 CI 先 Lighthouse；多地点再 Sitespeed |
| API 生成 | Orval：6,337 Star，2026-08-07 有推送 | Kubb：1,770 Star，2026-08-04 有推送 | Axios + Query 优先 Orval；schema 插件化选 Kubb |
| 未使用代码/依赖 | Knip：11,912 Star，2026-08-06 有推送 | depcheck：4,930 Star，已归档 | 只推荐 Knip |
| Admin 参考架构 | Ant Design Pro：38,641 Star | Refine：35,482 Star；React Admin：26,881 Star | 只借鉴权限、资源和布局模式，不重做现有项目 |
| 图表 | ECharts：67,003 Star，当前已集成 | Recharts：27,478 Star、完整包更小 | 保留 ECharts；优化异步 Chunk 和预算 |

### 开发效率工具

- VS Code：Oxc 官方扩展（与 oxlint 对齐）、Microsoft Playwright Test、Error Lens、Pretty TypeScript Errors。
- Chrome/浏览器：React Developer Tools Profiler、axe DevTools；Network/Performance/Lighthouse 使用浏览器内置面板即可。
- 应用内开发工具：`@tanstack/react-query-devtools` 仅在开发环境动态导入，观察缓存命中、重复 key 和失效范围。
- CLI：`rollup-plugin-visualizer`、Lighthouse CI、Knip；有 OpenAPI 后再引入 Orval。

## 四、行业最佳实践对标

| 对标项目 | 审计日状态 | 可借鉴模式 | 当前项目差距 |
|---|---|---|---|
| [ThingsBoard](https://github.com/thingsboard/thingsboard) | 22,203 Star，Angular 20，持续活跃 | 实时遥测订阅、Widget 隔离、权限化工作台 | 当前偏请求/响应式，尚无统一实时数据与组件隔离契约；是否需要取决于设备实时性 |
| [ERPNext](https://github.com/frappe/erpnext) | 37,780 Star，持续活跃 | 元数据驱动表单/单据、工作流、审计追踪 | 当前单据语义强，但基础档案/API 仍较多手工重复定义 |
| [Apache Superset](https://github.com/apache/superset) | 74,179 Star，React 18，持续活跃 | react-window、ECharts、bundle-stats、Playwright、插件式图表 | 当前已有 ECharts/Playwright，但缺虚拟化与 bundle PR 门禁 |
| Ant Design Pro / Refine | 38,641 / 35,482 Star | 资源路由、权限、列表/表单约定、数据 Provider | 当前业务定制度更高，不适合迁移；可吸收约定和生成能力 |

结论：项目并未落后到需要换脚手架。其领域拆分、严格 TS、Query 和测试基线优于多数中小型 MES；与成熟工业后台的主要差距是“性能/可访问性可量化门禁”“服务器状态一致性”“高密度表格虚拟化”“契约生成”，应沿现有架构渐进补齐。

## 五、总体评估

- 优化项总数：9 项（P0：1，P1：5，P2：3）。
- 推荐立即新增：3 个开发依赖/工具（`rollup-plugin-visualizer`、`@axe-core/playwright`、Knip）；条件式新增 2 个（Query Devtools、Orval）。第一阶段不建议新增运行时依赖。
- 预计总投入：核心优化约 16-24 人天；OpenAPI 全面迁移不计入该数字。
- 预计综合收益：初始 gzip 降低 16%-25%；入口业务代码 gzip 降低 34%-51%；高密度列表 DOM 降低 50%-68%；档案往返重复请求降低 30%-60%；关键页面 serious/critical A11y 自动化问题归零。
- 综合健康度：当前 **76/100**；完成 P0/P1 后预计 **87/100**；再完成性能门禁、契约生成试点和大组件拆分后预计 **90/100**。
- 评分是工程审计量表，不是生产 SLO。最终收益需要在生产构建、固定数据集、固定网络/硬件上复测。

## 六、优先执行路线图

1. 第 1-2 天：拆分认证启动 Query import；生成 bundle 图并建立入口体积基线。
2. 第 3 天：加入初始 gzip/最大 Chunk 预算；验证生产 `NODE_ENV` 和依赖重复来源。
3. 第 4-6 天：稳定加工单及基础档案 columns；在加工单 50 行场景试点 AntD 原生 virtual。
4. 第 7-10 天：迁移 customer/paper/machine/warehouse 详情与编辑查询；统一错误、重试、失效语义。
5. 第 11 天：修复认证用户同步，验证权限授予/撤销/离线恢复。
6. 第 12-14 天：修复对比度、路由焦点、标题层级、表单错误定位；接入 axe Playwright。
7. 第 15-20 天：拆分 RewindingConfigForm 与加工单 API/类型子域；补 model/hook 单测。
8. 后续试点：若后端 OpenAPI 质量合格，用 Orval 迁移一个基础档案领域；若单页真实数据超过 1,000 行，再评估独立数据网格。

## 七、需要补充确认的信息

1. `[待确认]` 生产 Nginx/CDN 是否开启 Brotli/gzip、长期缓存和内容哈希静态资源策略；当前体积目标按 gzip 估算。
2. `[待确认]` 工厂现场常用终端配置、网络延迟和浏览器最低版本；这决定虚拟化与性能预算的严苛程度。
3. `[待确认]` 加工单/出库/结算列表的典型与最大 pageSize，是否存在可变行高、展开行或 1,000+ 行客户端编辑需求。
4. `[待确认]` 权限变更要求是立即生效、下次聚焦生效还是重新登录生效；这决定 `/me` 的刷新和缓存策略。
5. `[待确认]` Spring Boot 是否已有完整、稳定的 OpenAPI 文档，以及是否允许把生成契约纳入 CI。
6. `[待确认]` 是否要求达到 WCAG 2.2 AA，以及现场是否需要键盘-only、NVDA/读屏器支持。
7. `[待确认]` 是否允许接入匿名 RUM/Web Vitals；若不允许，应建立内网合成监控页面和固定测试账号。
8. `[待确认]` 33 个被跳过的 E2E 是否会在 CI 提供凭据；若不会，当前端到端回归覆盖会被高估。
