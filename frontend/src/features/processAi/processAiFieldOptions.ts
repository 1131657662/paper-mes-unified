import type {
  ProcessAiAssignment,
  ProcessAiCompiledPlan,
  ProcessAiPackagingCandidate,
  ProcessAiParseResult,
  ProcessAiProcessMode,
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
  excludedIds: ReadonlySet<string> = new Set(),
): string[] {
  return groups.flatMap((group) => group.options.map((option) => option.id))
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
    return group.options.filter((option) => selected.has(option.id)
      || (option.required && option.category === 'PLAN'))
      .flatMap((option) => option.paths)
  }))]
}

function assignmentOptions(
  assignment: ProcessAiAssignment,
  compiledPlan: ProcessAiCompiledPlan | undefined,
  packaging?: ProcessAiPackagingCandidate,
): ProcessAiFieldOption[] {
  const base = `/assignments/${assignment.ownerRollRef}`
  const options = configurationOptions(assignment, base)
  if (compiledPlan?.plan.machineUuid) options.push(option(base, 'machine', '建议机台',
    compiledPlan.plan.machineUuid, [`${base}/machineUuid`], 'PLAN'))
  if (assignment.rewindIntent) addRewindOptions(options, base, assignment.rewindIntent)
  if (assignment.sawIntent) addSawOption(options, base, assignment.sawIntent)
  addCustomerSpecOption(options, base, assignment, compiledPlan)
  addAncillaryOptions(options, base, assignment.ancillaryRequirements, packaging)
  return options
}

function addCustomerSpecOption(
  options: ProcessAiFieldOption[],
  base: string,
  assignment: ProcessAiAssignment,
  compiledPlan?: ProcessAiCompiledPlan,
) {
  const specs = assignment.customerSpecs ?? []
  if (specs.length === 0) return
  const values = specs.map((spec) => {
    const fields = [spec.paperName, spec.gramWeight == null ? undefined : `${spec.gramWeight}g`,
      spec.finishWidth == null ? undefined : `${spec.finishWidth}mm`].filter(Boolean)
    return `第${spec.outputIndex + 1}件：${fields.join(' / ') || '客户规格'}${spec.overrideReason
      ? `（${spec.overrideReason}）` : ''}`
  })
  const paths = specs.flatMap((spec) => {
    const prefix = `${base}/customerSpecs/${spec.outputIndex}`
    return [spec.paperName != null ? `${prefix}/paperName` : undefined,
      spec.gramWeight != null ? `${prefix}/gramWeight` : undefined,
      spec.finishWidth != null ? `${prefix}/finishWidth` : undefined,
      spec.overrideReason != null ? `${prefix}/overrideReason` : undefined].filter(
        (value): value is string => value !== undefined)
  })
  if (paths.length === 0 || !compiledPlan) return
  options.push(option(base, 'customer-spec', '客户销售规格', values.join('；'), paths, 'PLAN'))
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
  if (!candidate) return '等待后端核验附加工艺'
  const quantity = candidate.billingBasis === 'TON' ? '当前母卷吨位' : '当前母卷件数'
  const price = candidate.billingMode === 3
    ? `固定 ${candidate.billingAmount ?? '-'} 元`
    : `${candidate.unitPrice ?? '未定价'} 元/${candidate.billingBasis === 'TON' ? '吨' : '件'}`
  return `${candidate.stepName} · ${quantity} · ${price}`
}

function configurationOptions(assignment: ProcessAiAssignment, base: string): ProcessAiFieldOption[] {
  const mode = assignment.processMode ?? legacyProcessMode(assignment.processType)
  const options = [option(base, 'scope', '适用母卷', rollScope(assignment),
    [`${base}/sourceRollRefs`, `${base}/coveredRollRefs`], 'PLAN'),
    option(base, 'process-mode', '加工方式', processModeText(mode),
      [`${base}/processMode`], 'PLAN')]
  if (assignment.processType === 'REWIND' || assignment.processType === 'SAW') {
    options.push(option(base, 'type', '主工艺', assignment.processType === 'SAW' ? '锯纸' : '复卷',
      [`${base}/processType`], 'PLAN'))
  }
  return options
}

function legacyProcessMode(type: ProcessAiAssignment['processType']): ProcessAiProcessMode {
  if (type === 'ANCILLARY_ONLY' || type === 'SERVICE_ONLY') return 'SERVICE_ONLY'
  if (type === 'DIRECT_SHIP') return 'DIRECT_SHIP'
  return 'STANDARD'
}

function processModeText(mode: ProcessAiProcessMode) {
  const labels: Record<ProcessAiProcessMode, string> = {
    STANDARD: '标准加工', ON_SITE: '现场定尺', DIRECT_SHIP: '不加工直发', SERVICE_ONLY: '仅附加工艺',
  }
  return labels[mode]
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
