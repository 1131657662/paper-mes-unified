import type { FinishProductionVO, RollProductionVO } from '../../../types/processOrder'
import { formatKg, formatTon } from '../../../utils/numberFormatters'
import { buildFinishLayers, layersSummaryText } from './layeredRewindView'
import type { FinishGroup } from './types'

/* ---------- 通用格式化 ---------- */

export const dict = (map: Record<number, string>, v?: number) =>
  v != null ? (map[v] ?? '-') : '-'

export const MAIN_STEP_TYPE: Record<number, string> = { 1: '锯纸', 2: '复卷' }
export const REWIND_MODE: Record<number, string> = {
  1: '改门幅不变直径',
  2: '改直径不变门幅',
  3: '改门幅 + 改直径',
  4: '内外层分层',
  5: '多母卷合并复卷',
}

export const fmt = (v?: number, suffix = '') => (v == null ? '-' : suffix ? `${v} ${suffix}` : `${v}`)
export const fmtKg = (v?: number) => (v == null ? '-' : formatKg(v))
export const toDisplayDiameterMm = (value?: number) =>
  value == null ? undefined : value > 0 && value < 100 ? Math.round(value * 25.4) : value
export const fmtDiameter = (value?: number, prefix = '') => {
  const diameter = toDisplayDiameterMm(value)
  return diameter == null ? '-' : `${prefix}${diameter} mm`
}

export const isActiveProductionFinish = (finish: FinishProductionVO) => finish.rollNoStatus !== 3
export const isRemainProductionFinish = (finish: FinishProductionVO) => finish.isRemain === 1
export const isSpareProductionFinish = (finish: FinishProductionVO) => finish.isSpare === 1
export const isDeliverableProductionFinish = (finish: FinishProductionVO) => (
  isActiveProductionFinish(finish) && !isSpareProductionFinish(finish) && !isRemainProductionFinish(finish)
)
export const isVisibleProductionOutput = (finish: FinishProductionVO) => (
  isActiveProductionFinish(finish) && !isSpareProductionFinish(finish)
)
export const isActiveSpareProductionFinish = (finish: FinishProductionVO) => (
  isActiveProductionFinish(finish) && isSpareProductionFinish(finish)
)

/** 从 rewindParams 取复卷模式标签 */
export const rewindModeLabel = (record: RollProductionVO) => {
  const mode = record.rewindParams?.find((p) => p.paramMode != null)?.paramMode
  if (mode != null) return REWIND_MODE[mode] ?? `复卷模式${mode}`
  return null
}

/* ---------- 成品分组 ---------- */

export function groupFinishes(finishes?: FinishProductionVO[]): FinishGroup[] {
  const official = (finishes ?? []).filter(isDeliverableProductionFinish)
  const map = new Map<number, { count: number; totalEstimate: number }>()
  for (const f of official) {
    const w = f.finishWidth ?? 0
    const entry = map.get(w) ?? { count: 0, totalEstimate: 0 }
    entry.count++
    entry.totalEstimate += f.estimateWeight ?? 0
    map.set(w, entry)
  }
  return Array.from(map.entries())
    .sort(([a], [b]) => b - a)
    .map(([width, { count, totalEstimate }]) => ({ width, count, totalEstimate }))
}

/* ---------- 修边计算 ---------- */

export function calcTrimWidth(record: RollProductionVO) {
  const explicitTrim = trimFinishes(record.finishes).reduce((sum, finish) => sum + (finish.finishWidth ?? 0), 0)
  if (explicitTrim > 0) return explicitTrim
  const legacyTrim = legacyTrimWidthFromFinishes(record.finishes)
  if (legacyTrim > 0) return legacyTrim
  if (!canInferTrimFromLayout(record)) return 0
  const originalWidth = record.originalWidth ?? 0
  if (!originalWidth) return 0
  const groups = groupFinishes(record.finishes)
  const used = groups.reduce((sum, g) => sum + g.width * g.count, 0)
  return Math.max(0, originalWidth - used)
}

function canInferTrimFromLayout(record: RollProductionVO) {
  if (record.processMode !== 1) return false
  if (record.mainStepType !== 2) return true
  const mode = record.rewindParams?.find((p) => p.paramMode != null)?.paramMode
  return mode !== 2
}

export function trimFinishes(finishes?: FinishProductionVO[]) {
  return (finishes ?? []).filter((finish) => isVisibleProductionOutput(finish) && isRemainProductionFinish(finish))
}

export function trimWeightFromFinishes(finishes?: FinishProductionVO[]) {
  const explicit = trimFinishes(finishes).reduce((sum, finish) => {
    return sum + (finish.actualWeight ?? finish.estimateWeight ?? 0)
  }, 0)
  if (explicit > 0) return explicit
  return (finishes ?? []).reduce((sum, finish) => sum + (finish.trimWeightShare ?? 0), 0)
}

export function legacyTrimWidthFromFinishes(finishes?: FinishProductionVO[]) {
  return (finishes ?? []).reduce((sum, finish) => sum + (finish.trimWidthShare ?? 0), 0)
}

/* ---------- 加工流程步骤描述 ---------- */

export interface ProcessingStepLine {
  header: string
  details: string[]
}

/** 构建单个 RollProductionVO 的加工流程描述 */
export function buildProcessingFlow(record: RollProductionVO): ProcessingStepLine[] {
  const result: ProcessingStepLine[] = []
  const params = record.rewindParams ?? []
  const rewindMode = params[0]?.paramMode
  const groups = groupFinishes(record.finishes)
  const isRewind = record.mainStepType === 2
  const trim = calcTrimWidth(record)
  const spareCount = (record.finishes ?? []).filter(isActiveSpareProductionFinish).length

  // 主工艺
  if (record.processMode === 3) {
    result.push({
      header: '① 直发（不加工）',
      details: ['原纸直接入库，不经过加工环节'],
    })
    return result
  }

  const hasConfig = params.length > 0 || groups.length > 0
  if (!hasConfig) {
    result.push({ header: '未配置加工方案', details: [] })
    return result
  }

  const mainLabel = isRewind ? '主复卷' : '主锯纸'
  const modeLabel = isRewind && rewindMode ? `: ${REWIND_MODE[rewindMode] ?? ''}` : ''
  const mainHeader = `① ${mainLabel}${modeLabel}`
  const mainDetails: string[] = []

  // 锯纸
  if (!isRewind) {
    const knifeCount = record.steps?.find((s) => s.isMain === 1)?.knifeCount ?? (groups.reduce((s, g) => s + g.count, 0) - 1)
    const widthLayout = groups.map((g) => `${g.width} mm × ${g.count}件`).join(', ')
    if (knifeCount > 0) mainDetails.push(`切 ${knifeCount}刀 | 门幅 ${widthLayout}`)
    else if (widthLayout) mainDetails.push(`门幅 ${widthLayout}`)
    if (trim > 0) mainDetails.push(`修边 ${trim} mm`)
    const totalCount = groups.reduce((s, g) => s + g.count, 0)
    const totalWeight = groups.reduce((s, g) => s + g.totalEstimate, 0)
    if (totalCount > 0) {
      const weightStr = totalWeight > 0 ? `，共 ${formatTon(totalWeight / 1000)}` : ''
      mainDetails.push(`预估产成品 ${totalCount}件${weightStr}`)
    }
  }

  // 复卷
  if (isRewind) {
    // 直径
    if (rewindMode === 2 || rewindMode === 3 || rewindMode === 5) {
      const diameter = params[0]?.outDiameter
      if (diameter != null) mainDetails.push(`成品直径 ≤ ${fmtDiameter(diameter)}`)
    }
    // 纸芯
    const core = params[0]?.coreDiameter
    if (core != null && (rewindMode === 2 || rewindMode === 3 || rewindMode === 4 || rewindMode === 5)) {
      mainDetails.push(`纸芯 ${core}"`)
    }
    // 重复
    if (rewindMode === 3) {
      const repeatCount = params.filter((p) => p.layerSort != null).length
      if (repeatCount > 1) mainDetails.push(`重复 ${repeatCount} 次`)
    }
    // 分层
    if (rewindMode === 4) {
      const layerText = layersSummaryText(buildFinishLayers(record, record.finishes ?? []))
      if (layerText) mainDetails.push(layerText)
      const segs = layerText ? '' : [...params].sort((a, b) => (a.layerSort ?? 0) - (b.layerSort ?? 0))
        .map((p) => fmt(p.layerWidth, 'mm')).join(' / ')
      if (segs) mainDetails.push(`${params.length}段: ${segs}`)
    }
    // 排刀
    if (groups.length > 0) {
      mainDetails.push(`排刀: ${groups.map((g) => `${g.width} mm × ${g.count}件`).join(', ')}${trim > 0 ? ` | 修边 ${trim} mm` : ''}`)
    }
    // mode 5 来源
    if (rewindMode === 5) {
      const sources = collectUniqueSources(record.finishes ?? [])
      if (sources.length > 0) {
        mainDetails.push(`来源: ${sources.map((s) => `${s.label}(${s.shareRatio}%)`).join(', ')}`)
      }
    }
    // 预估产量
    const totalCount = groups.reduce((s, g) => s + g.count, 0) + spareCount
    const totalWeight = groups.reduce((s, g) => s + g.totalEstimate, 0)
    if (totalCount > 0) {
      const weightStr = totalWeight > 0 ? `，共 ${formatTon(totalWeight / 1000)}` : ''
      mainDetails.push(`预估产成品 ${totalCount}件${weightStr}`)
    }
  }

  if (mainDetails.length > 0) result.push({ header: mainHeader, details: mainDetails })
  else result.push({ header: mainHeader, details: ['未配置规格'] })

  // 追加工序
  const additionalSteps = (record.steps ?? [])
    .filter((s) => s.isMain !== 1)
    .sort((a, b) => (a.stepSort ?? 0) - (b.stepSort ?? 0))
  for (let i = 0; i < additionalSteps.length; i++) {
    const step = additionalSteps[i]
    const numChar = String.fromCodePoint(0x2460 + i + 1) // ①=2460
    const label = step.stepType === 1 ? '追切纸' : step.stepName || '追加'
    const header = `${numChar} ${label}`
    const details: string[] = []
    if (step.stepType === 1 && step.knifeCount != null) {
      details.push(`切 ${step.knifeCount}刀`)
    }
    if (step.processWeight != null) {
      details.push(`加工吨位 ${formatTon(step.processWeight)}`)
    }
    if (step.unitPrice != null) {
      details.push(`单价 ¥${step.unitPrice}${step.stepType === 1 ? '/刀' : '/吨'}`)
    }
    if (step.remark) details.push(step.remark)
    if (details.length > 0) result.push({ header, details })
    else result.push({ header, details: [] })
  }

  return result
}

/* ---------- 跨卷来源汇总 ---------- */

interface SourceSummary { originalUuid: string; label: string; shareRatio: number }

function collectUniqueSources(finishes: FinishProductionVO[]): SourceSummary[] {
  const map = new Map<string, SourceSummary>()
  for (const f of finishes) {
    for (const s of f.sources ?? []) {
      if (!s.originalUuid) continue
      if (!map.has(s.originalUuid)) {
        map.set(s.originalUuid, {
          originalUuid: s.originalUuid,
          label: s.rollNo || s.paperName || s.originalUuid,
          shareRatio: s.shareRatio ?? 0,
        })
      }
    }
  }
  return Array.from(map.values())
}

/* ---------- 分段/直径条件文本 ---------- */

export function buildConditionText(record: RollProductionVO): string {
  if (record.processMode === 3) return '直发'

  const params = record.rewindParams ?? []
  const hasConfig = params.length > 0 || (record.finishes?.length ?? 0) > 0
  if (!hasConfig) return '-'

  // 锯纸
  if (record.mainStepType !== 2) {
    const mainStep = record.steps?.find((s) => s.isMain === 1)
    if (mainStep?.knifeCount) return `${mainStep.knifeCount}刀`
    return '-'
  }

  // 复卷
  const mode = params[0]?.paramMode
  const parts: string[] = []

  // 改门幅的排布在“门幅排布”中展示；areaRatio 当前存的是预估重量，不能当百分比显示。
  if (mode === 1) {
    parts.push('按门幅排布')
  }

  // 直径 & 纸芯 (mode 2, 3, 5)
  if (mode === 2 || mode === 3 || mode === 5) {
    const d = params[0]?.outDiameter
    if (d != null) parts.push(`φ≤${fmtDiameter(d)}`)
    const core = params[0]?.coreDiameter
    if (core != null && core !== 3) parts.push(`芯${core}"`)
  }

  // 分层 (mode 4)
  if (mode === 4) {
    const layerText = layersSummaryText(buildFinishLayers(record, record.finishes ?? []))
    if (layerText) return '内外层分层'
    const sorted = [...params].sort((a, b) => (a.layerSort ?? 0) - (b.layerSort ?? 0))
    parts.push(sorted.map((p) => `${p.layerWidth} mm`).join(' / '))
  }

  // 合并 (mode 5)
  if (mode === 5) parts.push('合并')

  return parts.join(' · ') || '-'
}

/* ---------- 排刀方案文本 ---------- */

export function buildLayoutText(record: RollProductionVO): string {
  const layerText = layersSummaryText(buildFinishLayers(record, record.finishes ?? []))
  if (layerText) return layerText
  const groups = groupFinishes(record.finishes)
  if (!groups.length) return '-'
  return groups.map((g) => `${g.width} mm × ${g.count}`).join(' + ')
}
