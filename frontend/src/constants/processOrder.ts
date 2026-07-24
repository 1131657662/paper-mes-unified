/** 加工单状态字典：0草稿 1待下发 2加工中 3待回录 4已完成 5已结算 6已作废。 */
export const ORDER_STATUS: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待下发', color: 'orange' },
  2: { text: '加工中', color: 'processing' },
  3: { text: '待回录', color: 'orange' },
  4: { text: '已完成', color: 'success' },
  5: { text: '已结算', color: 'blue' },
  6: { text: '已作废', color: 'error' },
}

/** 优先级字典：1普通 2加急 3特急。 */
export const PRIORITY: Record<number, string> = { 1: '普通', 2: '加急', 3: '特急' }

/** 加工方式字典：1标准加工 2现场定尺 3不加工直发 4仅附加工艺。 */
export const PROCESS_MODE: Record<number, string> = {
  1: '标准加工',
  2: '现场定尺',
  3: '不加工直发',
  4: '仅附加工艺',
}

export function processModeRequiresMain(processMode?: number): boolean {
  return processMode !== 3 && processMode !== 4
}

/** 原纸卷状态字典：1待加工 2加工中 3完成 4直发 5报废。 */
export const ROLL_STATUS: Record<number, string> = {
  1: '待加工',
  2: '加工中',
  3: '完成',
  4: '直发',
  5: '报废',
}

/** 是否开票字典：1开票 2不开票。 */
export const IS_INVOICE: Record<number, string> = { 1: '开票', 2: '不开票' }

/** 本单结算方式：1次结 2月结。 */
export const ORDER_SETTLE_TYPE: Record<number, string> = { 1: '次结', 2: '月结' }

/** 成品卷状态字典：1待入库 2已入库 3已出库 4报废。 */
export const FINISH_STATUS: Record<number, { text: string; color: string }> = {
  1: { text: '待入库', color: 'orange' },
  2: { text: '已入库', color: 'blue' },
  3: { text: '已出库', color: 'success' },
  4: { text: '报废', color: 'error' },
}

/** 成品来源：服务型加工单独标识，避免与直发或锯纸/复卷产出混淆。 */
export const FINISH_SOURCE_TYPE: Record<number, { text: string; color: string }> = {
  1: { text: '加工产出', color: 'blue' },
  2: { text: '直发原纸', color: 'green' },
  3: { text: '整理成品', color: 'cyan' },
}

/** 工序类型字典：生产工艺与可独立计费的服务工序。 */
export const STEP_TYPE: Record<number, string> = {
  1: '锯纸',
  2: '复卷',
  3: '剥损整理',
  4: '重新包装',
}

export const DEFAULT_WIDTH_DIFFERENCE_POLICY: WidthDifferencePolicy = 'ALLOCATE'
export const WIDTH_DIFFERENCE_POLICY_OPTIONS = [
  { label: '分摊', value: 'ALLOCATE' },
  { label: '留余料', value: 'REMAINDER' },
  { label: '计损耗', value: 'LOSS' },
] satisfies Array<{ label: string; value: WidthDifferencePolicy }>

/**
 * 加工单状态机合法流转目标（来自后端 OrderStatus）。
 * 1→2或1→0(回退)；2→3或2→1(回退)；3→4或3→1(回退)；4→5或4→3(回退)；5终态。
 */
export const ORDER_STATUS_TRANSITIONS: Record<number, number[]> = {
  0: [1],
  1: [2, 0],
  2: [3, 1],
  3: [4, 1],
  4: [5, 3],
  5: [],
  6: [],
}

/** from→to 的流转动作文案，驱动操作列按钮。 */
export const TRANSITION_LABEL: Record<string, string> = {
  '0-1': '提交',
  '1-2': '下发',
  '1-0': '回退草稿',
  '2-3': '转待回录',
  '2-1': '回退待下发',
  '3-4': '完成回录',
  '3-1': '回退待下发',
  '4-5': '结算',
  '4-3': '回退待回录',
}

/** 三级闭合校验结果等级：PASS/WARN/BLOCK。 */
export const CLOSE_LEVEL: Record<string, { text: string; color: string }> = {
  PASS: { text: '闭合通过', color: 'success' },
  WARN: { text: '强警告', color: 'warning' },
  BLOCK: { text: '超差拦截', color: 'error' },
}

/** 是否正品/边角余料：0正品 1边角余料。 */
export const IS_REMAIN: Record<number, string> = { 0: '正品', 1: '边角余料' }

/** 卷号状态：1预生成 3作废（无2已使用）。 */
export const ROLL_NO_STATUS: Record<number, { text: string; color: string }> = {
  1: { text: '预生成', color: 'blue' },
  3: { text: '作废', color: 'default' },
}
import type { WidthDifferencePolicy } from '../types/processOrder'
