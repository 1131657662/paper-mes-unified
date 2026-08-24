import type { ProcessPlanDTO } from '../../types/processOrder'
import { buildProcessAiFieldOptions, type ProcessAiFieldOption } from './processAiFieldOptions'
import type {
  ProcessAiBaselinePlan,
  ProcessAiCurrentDraft,
  ProcessAiParseResult,
} from './types'

export interface ProcessAiReviewOption extends ProcessAiFieldOption {
  aiValue: string
  baselineValue: string
  currentValue: string
  conflict: boolean
}

export interface ProcessAiReviewGroup {
  ownerRollRef: string
  options: ProcessAiReviewOption[]
}

export interface ProcessAiRemarkReview {
  baselineValue: string
  currentValue: string
  proposedValue: string
  conflict: boolean
}

export function buildProcessAiReviewGroups(
  result: ProcessAiParseResult,
  current: ProcessAiCurrentDraft,
): ProcessAiReviewGroup[] {
  const groups = buildProcessAiFieldOptions(result)
  return groups.map((group) => {
    const baseline = result.baseline?.plans.find((item) => item.ownerRollRef === group.ownerRollRef)
    const candidate = result.compiled.plans.find((item) => item.ownerRollRef === group.ownerRollRef)
    const roll = current.rolls.find((item) => item.uuid === baseline?.originalUuid
      || item.uuid === candidate?.originalUuid)
    const currentPlan = roll ? current.plans[roll.localId] : undefined
    return {
      ownerRollRef: group.ownerRollRef,
      options: group.options.map((option) => reviewOption(
        option, baseline, currentPlan, currentPlan?.mainStepType ?? roll?.mainStepType,
        candidate?.plan,
      )),
    }
  })
}

export function conflictingOptionIds(groups: ProcessAiReviewGroup[]): Set<string> {
  const result = new Set<string>()
  for (const group of groups) {
    const typeConflict = group.options.some((option) => option.id.endsWith('/type') && option.conflict)
    for (const option of group.options) {
      if (typeConflict || option.conflict) result.add(option.id)
    }
  }
  return result
}

export function buildProcessAiRemarkReview(
  result: ProcessAiParseResult,
  currentRemark: string | undefined,
  conversationRequirement: string,
): ProcessAiRemarkReview {
  const baseline = normalizeText(result.baseline?.remarkLong)
  const current = normalizeText(currentRemark)
  const conversation = normalizeText(conversationRequirement)
  const proposed = current || conversation || baseline
  return {
    baselineValue: baseline,
    currentValue: current,
    proposedValue: proposed,
    conflict: Boolean(current && conversation && current !== conversation),
  }
}

function reviewOption(
  option: ProcessAiFieldOption,
  baseline: ProcessAiBaselinePlan | undefined,
  current: ProcessPlanDTO | undefined,
  currentMainStepType: number | undefined,
  candidate: ProcessPlanDTO | undefined,
): ProcessAiReviewOption {
  const kind = option.id.slice(option.id.lastIndexOf('/') + 1)
  const baseValue = planValue(
    kind, baseline?.plan, baseline?.plan?.mainStepType ?? baseline?.mainStepType,
    baseline?.route ?? false,
  )
  const currentValue = planValue(kind, current, currentMainStepType, false)
  const aiValue = planValue(kind, candidate, candidate?.mainStepType, false)
  const hasBaseline = hasBaselineValue(kind, baseline)
  return {
    ...option,
    required: option.required || requiresCompleteNewPlan(kind, baseline, candidate),
    aiValue: aiValue.text === '-' ? option.detail : aiValue.text,
    baselineValue: hasBaseline ? baseValue.text : '旧解析结果未保存基线',
    currentValue: currentValue.text,
    conflict: hasBaseline && baseValue.key !== currentValue.key && currentValue.key !== aiValue.key,
  }
}

function requiresCompleteNewPlan(
  kind: string,
  baseline: ProcessAiBaselinePlan | undefined,
  candidate: ProcessPlanDTO | undefined,
) {
  if (baseline?.plan !== undefined && baseline.plan.mainStepType === candidate?.mainStepType) return false
  if (baseline?.plan === undefined && baseline && (baseline.processMode === 3 || baseline.processMode === 4)) return false
  return ['scope', 'process-mode', 'type', 'machine', 'rewind-mode', 'diameter', 'core', 'width', 'saw',
    'customer-spec'].includes(kind)
}

function hasBaselineValue(kind: string, baseline: ProcessAiBaselinePlan | undefined) {
  if (!baseline) return false
  if (kind === 'scope' || kind === 'type') return true
  return baseline.plan !== undefined
}

function planValue(kind: string, plan: ProcessPlanDTO | undefined,
                   mainStepType: number | undefined, route: boolean) {
  if (route) return value('链式工艺', { route: true })
  if (kind === 'scope') return value('同一母卷范围', 'scope')
  if (kind === 'type') return value(processTypeText(mainStepType), mainStepType ?? null)
  if (kind === 'machine') return value(plan?.machineUuid ?? '-', plan?.machineUuid ?? null)
  if (kind === 'rewind-mode') return value(plan?.rewindMode ? `模式 ${plan.rewindMode}` : '-', plan?.rewindMode ?? null)
  if (kind === 'diameter') return diameterValue(plan)
  if (kind === 'core') return coreValue(plan)
  if (kind === 'width') return widthValue(plan)
  if (kind === 'saw') return sawValue(plan)
  return value('-', null)
}

function diameterValue(plan?: ProcessPlanDTO) {
  const data = plan?.segments?.map((item) => ({
    ratio: item.segmentRatio, diameter: item.targetDiameter, repeat: item.repeatCount,
  })) ?? []
  const text = data.length ? data.map((item) => `${item.diameter ?? '-'}mm / ${item.ratio ?? '-'}%`).join('；') : '-'
  return value(text, { allocationRule: plan?.allocationRule, segments: data })
}

function coreValue(plan?: ProcessPlanDTO) {
  const cores = [...new Set(plan?.segments?.map((item) => item.finishCoreDiameter) ?? [])]
  return value(cores.length ? `${cores.join(' / ')} 英寸` : '-', cores)
}

function widthValue(plan?: ProcessPlanDTO) {
  const layouts = plan?.segments?.map((segment) => segment.layoutItems?.map((item) => ({
    width: item.width, quantity: item.quantity, itemType: item.itemType,
  })) ?? []) ?? []
  const text = layouts.flat().length
    ? layouts.map((items) => items.map(layoutItemText).join(' + ')).join('；')
    : '-'
  return value(text, { layouts, policy: plan?.widthDifferencePolicy })
}

function layoutItemText(item: { width?: number; quantity?: number; itemType?: string }) {
  const text = `${item.width ?? '-'}x${item.quantity ?? 1}`
  return item.itemType === 'TRIM' ? `余料 ${text}` : text
}

function sawValue(plan?: ProcessPlanDTO) {
  const specs = plan?.finishSpecs?.map((item) => ({
    width: item.finishWidth, count: item.count, itemType: item.itemType,
  })) ?? []
  const text = specs.length
    ? `${plan?.knifeCount ?? 0} 刀；${specs.map(sawItemText).join(' + ')}`
    : '-'
  return value(text, { knifeCount: plan?.knifeCount, specs })
}

function sawItemText(item: { width?: number; count?: number; itemType?: string }) {
  const text = `${item.width ?? '-'}x${item.count ?? 1}`
  return item.itemType === 'TRIM' ? `余料 ${text}` : text
}

function value(text: string, comparable: unknown) {
  return { text, key: JSON.stringify(comparable) }
}

function processTypeText(mainStepType?: number) {
  if (mainStepType === 1) return '锯纸'
  if (mainStepType === 2) return '复卷'
  return '-'
}

function normalizeText(value: string | undefined) {
  return value?.trim() ?? ''
}
