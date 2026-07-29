import type { FinishRoll } from '../../../types/processOrder'
import type {
  PrintAnnotation,
  PrintAnnotationField,
  PrintRollBlock,
  PrintRouteOutput,
  PrintSheetModel,
} from './printPreviewTypes'

interface FinishPlacement {
  blockKey: string
  outputKey: string
  finish?: FinishRoll
}

type EligiblePlacement = FinishPlacement & { finish: FinishRoll }

interface FieldValue {
  effective?: string | number
  overridden: boolean
}

const FIELDS = ['paperName', 'gramWeight', 'finishWidth'] as const

export function applyPrintAnnotations(
  finishRolls: FinishRoll[],
  blocks: PrintRollBlock[],
): PrintSheetModel {
  const placements = collectPlacements(finishRolls, blocks)
  const orderAnnotations: PrintAnnotation[] = []
  const blockAnnotations = new Map<string, PrintAnnotation[]>()
  const outputAnnotations = new Map<string, PrintAnnotation[]>()

  for (const field of FIELDS) {
    assignFieldAnnotations({ field, placements, orderAnnotations, blockAnnotations, outputAnnotations })
  }
  return {
    orderAnnotations,
    blocks: attachAnnotations(blocks, blockAnnotations, outputAnnotations),
  }
}

interface AssignOptions {
  field: PrintAnnotationField
  placements: FinishPlacement[]
  orderAnnotations: PrintAnnotation[]
  blockAnnotations: Map<string, PrintAnnotation[]>
  outputAnnotations: Map<string, PrintAnnotation[]>
}

function assignFieldAnnotations(options: AssignOptions) {
  const known = options.placements.filter(hasEligibleFinish)
  if (!known.some((placement) => fieldValue(placement.finish, options.field).overridden)) return
  const orderValue = uniformValue(options.placements, options.field)
  if (orderValue != null) {
    options.orderAnnotations.push(annotation(options.field, orderValue))
    return
  }
  assignBlockAnnotations(options, known)
}

function assignBlockAnnotations(options: AssignOptions, known: EligiblePlacement[]) {
  const blocks = new Set(options.placements.map((placement) => placement.blockKey))
  for (const blockKey of blocks) {
    const allInBlock = options.placements.filter((placement) => placement.blockKey === blockKey)
    const knownInBlock = known.filter((placement) => placement.blockKey === blockKey)
    if (!knownInBlock.some((placement) => fieldValue(placement.finish, options.field).overridden)) continue
    const blockValue = uniformValue(allInBlock, options.field)
    if (blockValue != null) {
      append(options.blockAnnotations, blockKey, annotation(options.field, blockValue))
      continue
    }
    assignOutputAnnotations(options, knownInBlock)
  }
}

function assignOutputAnnotations(options: AssignOptions, placements: EligiblePlacement[]) {
  for (const placement of placements) {
    const value = fieldValue(placement.finish, options.field)
    if (!value.overridden || value.effective == null) continue
    append(options.outputAnnotations, placement.outputKey, annotation(options.field, value.effective))
  }
}

function collectPlacements(finishRolls: FinishRoll[], blocks: PrintRollBlock[]): FinishPlacement[] {
  const finishByUuid = new Map(finishRolls.map((finish) => [finish.uuid, finish]))
  const placements = blocks.flatMap((block) => finalOutputs(block).map((output) => {
    const finishUuid = output.finishRollUuid ?? output.key
    return { blockKey: block.key, outputKey: output.key, finish: finishByUuid.get(finishUuid) }
  }))
  return placements.filter((placement) => placement.finish == null || isEligibleFinish(placement.finish))
}

function finalOutputs(block: PrintRollBlock): PrintRouteOutput[] {
  return block.routeStages.flatMap((stage) => stage.outputs.filter((output) => output.status === 'final'))
}

function hasEligibleFinish(placement: FinishPlacement): placement is EligiblePlacement {
  return placement.finish != null && isEligibleFinish(placement.finish)
}

function isEligibleFinish(finish: FinishRoll) {
  return finish.rollNoStatus !== 3 && finish.isSpare !== 1 && finish.isRemain !== 1
}

function uniformValue(placements: FinishPlacement[], field: PrintAnnotationField) {
  const eligible = placements.filter(hasEligibleFinish)
  if (!placements.length || eligible.length !== placements.length) return undefined
  const values = eligible.map((placement) => fieldValue(placement.finish, field).effective)
  const first = values[0]
  if (first == null || values.some((value) => value !== first)) return undefined
  return first
}

function fieldValue(finish: FinishRoll, field: PrintAnnotationField): FieldValue {
  if (field === 'paperName') {
    const physical = normalizeText(finish.paperName)
    const customer = normalizeText(finish.customerPaperName)
    return { effective: customer ?? physical, overridden: customer != null && customer !== physical }
  }
  const physical = field === 'gramWeight' ? finish.gramWeight : finish.finishWidth
  const customer = field === 'gramWeight' ? finish.customerGramWeight : finish.customerFinishWidth
  return { effective: customer ?? physical, overridden: customer != null && customer !== physical }
}

function annotation(field: PrintAnnotationField, value: string | number): PrintAnnotation {
  return { field, value: String(value) }
}

function append(map: Map<string, PrintAnnotation[]>, key: string, value: PrintAnnotation) {
  map.set(key, [...(map.get(key) ?? []), value])
}

function attachAnnotations(
  blocks: PrintRollBlock[],
  blockAnnotations: Map<string, PrintAnnotation[]>,
  outputAnnotations: Map<string, PrintAnnotation[]>,
): PrintRollBlock[] {
  return blocks.map((block) => ({
    ...block,
    annotations: blockAnnotations.get(block.key),
    routeStages: block.routeStages.map((stage) => ({
      ...stage,
      outputs: stage.outputs.map((output) => ({
        ...output,
        annotations: outputAnnotations.get(output.key),
      })),
    })),
  }))
}

function normalizeText(value?: string) {
  const normalized = value?.trim()
  return normalized || undefined
}
