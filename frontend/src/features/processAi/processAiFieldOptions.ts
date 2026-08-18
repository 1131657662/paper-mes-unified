import type {
  ProcessAiAssignment,
  ProcessAiCompiledPlan,
  ProcessAiPackagingCandidate,
  ProcessAiParseResult,
} from './types'

export interface ProcessAiFieldOption {
  id: string
  label: string
  detail: string
  paths: string[]
  category: 'PLAN' | 'ANCILLARY'
  required?: boolean
}

export interface ProcessAiAssignmentOptions {
  ownerRollRef: string
  options: ProcessAiFieldOption[]
}

export function buildProcessAiFieldOptions(result: ProcessAiParseResult): ProcessAiAssignmentOptions[] {
  return result.result.assignments.map((assignment) => ({
    ownerRollRef: assignment.ownerRollRef,
    options: assignmentOptions(
      assignment,
      result.compiled.plans.find((candidate) => candidate.ownerRollRef === assignment.ownerRollRef),
      result.compiled.packagingCandidates.find((candidate) => candidate.ownerRollRef === assignment.ownerRollRef),
    ),
  }))
}

export function defaultAcceptedOptionIds(
  groups: ProcessAiAssignmentOptions[],
  defaultOwnerRollRef?: string,
  excludedIds: ReadonlySet<string> = new Set(),
): string[] {
  return groups.filter((group) => !defaultOwnerRollRef || group.ownerRollRef === defaultOwnerRollRef)
    .flatMap((group) => group.options.map((option) => option.id))
    .filter((id) => !excludedIds.has(id))
}

export function acceptedPaths(
  groups: ProcessAiAssignmentOptions[],
  selectedIds: string[],
): string[] {
  const selected = new Set(selectedIds)
  return [...new Set(groups.flatMap((group) => {
    const selectedOptions = group.options.filter((option) => selected.has(option.id))
    if (selectedOptions.length === 0) return []
    const planEnabled = selectedOptions.some((option) => option.category === 'PLAN')
    return group.options.filter((option) => selected.has(option.id)
      || (planEnabled && option.required && option.category === 'PLAN'))
      .flatMap((option) => option.paths)
  }))]
}

function assignmentOptions(
  assignment: ProcessAiAssignment,
  compiledPlan: ProcessAiCompiledPlan | undefined,
  packaging?: ProcessAiPackagingCandidate,
): ProcessAiFieldOption[] {
  const base = `/assignments/${assignment.ownerRollRef}`
  const options = [
    option(base, 'scope', '适用母卷', rollScope(assignment),
      [`${base}/sourceRollRefs`, `${base}/coveredRollRefs`], 'PLAN', true),
    option(base, 'type', '主工艺', assignment.processType === 'SAW' ? '锯纸' : '复卷',
      [`${base}/processType`], 'PLAN', true),
  ]
  if (compiledPlan?.plan.machineUuid) options.push(option(base, 'machine', '建议机台',
    compiledPlan.plan.machineUuid, [`${base}/machineUuid`], 'PLAN'))
  if (assignment.rewindIntent) addRewindOptions(options, base, assignment.rewindIntent)
  if (assignment.sawIntent) addSawOption(options, base, assignment.sawIntent)
  addAncillaryOptions(options, base, assignment.ancillaryRequirements, packaging)
  return options
}

function addRewindOptions(options: ProcessAiFieldOption[], base: string, intent: Record<string, unknown>) {
  const rewind = `${base}/rewindIntent`
  options.push(option(base, 'rewind-mode', '复卷模式', textValue(intent.modeIntent),
    [`${rewind}/modeIntent`], 'PLAN'))
  const diameter = recordValue(intent.diameterRule)
  if (diameter) options.push(option(base, 'diameter', '直径与分卷', diameterDetail(diameter),
    presentPaths(`${rewind}/diameterRule`, diameter, ['type', 'parts', 'ratios', 'targetDiameter']), 'PLAN'))
  if (recordValue(intent.core)) options.push(option(base, 'core', '成品纸芯', measurementText(intent.core),
    [`${rewind}/core`], 'PLAN'))
  const width = recordValue(intent.widthRule)
  if (width) options.push(option(base, 'width', '成品门幅', widthDetail(width),
    presentPaths(`${rewind}/widthRule`, width, ['type', 'values', 'knifeCount']), 'PLAN'))
}

function addSawOption(options: ProcessAiFieldOption[], base: string, intent: Record<string, unknown>) {
  const saw = `${base}/sawIntent`
  options.push(option(base, 'saw', '锯纸方案', sawDetail(intent),
    presentPaths(saw, intent, ['type', 'knifeCount', 'widths']), 'PLAN'))
}

function addAncillaryOptions(
  options: ProcessAiFieldOption[],
  base: string,
  value: Record<string, unknown> | undefined,
  packaging?: ProcessAiPackagingCandidate,
) {
  if (!value) return
  if (recordValue(value.label)) options.push(option(base, 'label', '标签要求', '写入加工要求备注',
    [`${base}/ancillaryRequirements/label`], 'ANCILLARY'))
  if (recordValue(value.packaging)) options.push(option(base, 'packaging', '包装要求', packagingDetail(packaging),
    [`${base}/ancillaryRequirements/packaging`], 'ANCILLARY'))
}

function packagingDetail(candidate?: ProcessAiPackagingCandidate) {
  if (!candidate) return '待人工确认数量、价格和机台'
  const quantity = candidate.serviceQuantity == null
    ? '数量待填'
    : `${candidate.serviceQuantity} ${candidate.billingBasis === 'TON' ? '吨' : '件'}`
  const price = candidate.billingMode === 3
    ? `固定 ${candidate.billingAmount ?? '-'} 元`
    : `${candidate.unitPrice ?? '待定'} 元/${candidate.billingBasis === 'TON' ? '吨' : '件'}`
  return `${candidate.stepName} · ${quantity} · ${price} · 机台待确认`
}

function option(
  base: string, id: string, label: string, detail: string, paths: string[],
  category: 'PLAN' | 'ANCILLARY', required = false,
): ProcessAiFieldOption {
  return { id: `${base}/${id}`, label, detail, paths, category, required }
}

function rollScope(assignment: ProcessAiAssignment) {
  return [...assignment.sourceRollRefs, ...assignment.coveredRollRefs].join('、')
}

function diameterDetail(value: Record<string, unknown>) {
  if (value.type === 'WEIGHT_SPLIT') return `按重量分成 ${numberValue(value.parts) ?? 2} 卷`
  return recordValue(value.targetDiameter) ? measurementText(value.targetDiameter) : textValue(value.type)
}

function widthDetail(value: Record<string, unknown>) {
  if (Array.isArray(value.values)) return `${value.values.join(' + ')} mm`
  if (typeof value.knifeCount === 'number') return `切 ${value.knifeCount} 刀，均匀分配`
  return textValue(value.type)
}

function sawDetail(value: Record<string, unknown>) {
  if (Array.isArray(value.widths)) return `${value.widths.join(' + ')} mm`
  return `切 ${numberValue(value.knifeCount) ?? 0} 刀，均匀分配`
}

function measurementText(value: unknown) {
  const measurement = recordValue(value)
  if (!measurement) return '-'
  return `${numberValue(measurement.value) ?? '-'} ${textValue(measurement.unit)}`
}

function presentPaths(base: string, value: Record<string, unknown>, keys: string[]) {
  return keys.filter((key) => value[key] !== null && value[key] !== undefined)
    .map((key) => `${base}/${key}`)
}

function recordValue(value: unknown): Record<string, unknown> | undefined {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
    ? value as Record<string, unknown> : undefined
}

function textValue(value: unknown) {
  return typeof value === 'string' ? value : '-'
}

function numberValue(value: unknown) {
  return typeof value === 'number' ? value : undefined
}
