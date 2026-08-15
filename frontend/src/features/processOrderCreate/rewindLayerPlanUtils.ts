import type {
  FinishLayerDTO,
  ProcessPlanDTO,
  RewindLayoutItemPlanDTO,
  RewindSegmentPlanDTO,
} from '../../types/processOrder'
import type { RollDraft } from './types'
import { DEFAULT_WIDTH_DIFFERENCE_POLICY } from '../../constants/processOrder'
import { rewindWidthPolicy } from './rewindWidthUsage'

export function effectiveRollWidth(roll: Pick<RollDraft, 'actualWidth' | 'originalWidth'>): number {
  return roll.actualWidth != null && roll.actualWidth > 0 ? roll.actualWidth : roll.originalWidth
}

export function sameSpecRewindError(
  roll: Pick<RollDraft, 'originalDiameter' | 'coreDiameter'>,
): string | undefined {
  if (roll.originalDiameter == null || roll.originalDiameter <= 0) {
    return '母卷直径未维护，不能配置同规格复卷'
  }
  if (roll.coreDiameter == null || roll.coreDiameter <= 0) {
    return '母卷纸芯未维护，不能配置同规格复卷'
  }
  return undefined
}

export function defaultRewindSegment(roll: RollDraft, sort = 1): RewindSegmentPlanDTO {
  return {
    segmentSort: sort,
    segmentRatio: 1,
    targetDiameter: roll.originalDiameter,
    finishCoreDiameter: roll.coreDiameter ?? 3,
    repeatCount: 1,
    sources: roll.uuid ? [{ originalUuid: roll.uuid, shareRatio: 100, consumeRatio: 100, sourceSort: 1 }] : [],
    layoutItems: [{ width: effectiveRollWidth(roll), quantity: 1, itemType: 'FINISH' }],
  }
}

export function sameSpecRewindPlan(plan: ProcessPlanDTO, roll: RollDraft): ProcessPlanDTO {
  const segment = defaultRewindSegment(roll)
  return {
    ...plan,
    rewindMode: 6,
    widthDifferencePolicy: undefined,
    segments: [{
      ...segment,
      finishCoreDiameter: roll.coreDiameter,
      layoutItems: [{ width: effectiveRollWidth(roll), quantity: 1, itemType: 'FINISH' }],
    }],
  }
}

export function normalizeRewindPlan(plan: ProcessPlanDTO, roll: RollDraft): ProcessPlanDTO {
  const mode = plan.rewindMode
  if (!mode) return plan
  if (mode === 6) return sameSpecRewindPlan(plan, roll)
  const source = plan.segments?.length ? plan.segments : [defaultRewindSegment(roll)]
  const segments = source.map((segment, index) => ({
    ...normalizeSegmentForMode(segment, roll, mode),
    segmentSort: index + 1,
  }))
  return {
    ...plan,
    rewindMode: mode,
    widthDifferencePolicy: rewindWidthPolicy(mode).enabled
      ? plan.widthDifferencePolicy ?? DEFAULT_WIDTH_DIFFERENCE_POLICY
      : undefined,
    segments: normalizeSegmentRatios(segments),
  }
}

export function planWithRewindMode(
  plan: ProcessPlanDTO,
  roll: RollDraft,
  rewindMode: number,
): ProcessPlanDTO {
  return normalizeRewindPlan({ ...plan, rewindMode }, roll)
}

export function segmentRatioPercent(
  segment: RewindSegmentPlanDTO,
  segments: RewindSegmentPlanDTO[],
): number {
  const total = segments.reduce((sum, item) => sum + positiveRatio(item), 0)
  return total > 0 ? Math.round((positiveRatio(segment) / total) * 1000) / 10 : 0
}

export function normalizeLayeredRewindPlan(plan: ProcessPlanDTO, roll: RollDraft): ProcessPlanDTO {
  if (plan.rewindMode !== 4) return plan
  return normalizeRewindPlan(plan, roll)
}

export function normalizeLayeredRewindSegments(
  segments: RewindSegmentPlanDTO[] | undefined,
  roll: RollDraft,
): RewindSegmentPlanDTO[] {
  const sourceSegments = segments?.length ? segments : [defaultRewindSegment(roll)]
  return sourceSegments.map((segment) => normalizeLayeredRewindSegment(segment, roll))
}

function normalizeLayeredRewindSegment(
  segment: RewindSegmentPlanDTO,
  roll: RollDraft,
): RewindSegmentPlanDTO {
  const layoutItems = segment.layoutItems?.length
    ? segment.layoutItems
    : defaultRewindSegment(roll).layoutItems

  return {
    ...segment,
    targetDiameter: undefined,
    finishCoreDiameter: undefined,
    layoutItems: layoutItems?.map((item) => normalizeLayeredRewindItem(item, segment, roll)),
  }
}

function normalizeLayeredRewindItem(
  item: RewindLayoutItemPlanDTO,
  segment: RewindSegmentPlanDTO,
  roll: RollDraft,
): RewindLayoutItemPlanDTO {
  if (item.itemType === 'TRIM' || item.layers?.length) return item
  return { ...item, layers: [defaultLayerForSegment(segment, roll)] }
}

function defaultLayerForSegment(segment: RewindSegmentPlanDTO, roll: RollDraft): FinishLayerDTO {
  return {
    outDiameter: segment.targetDiameter ?? roll.originalDiameter,
    coreDiameter: segment.finishCoreDiameter ?? roll.coreDiameter ?? 3,
  }
}

function normalizeSegmentForMode(
  segment: RewindSegmentPlanDTO,
  roll: RollDraft,
  mode: number,
): RewindSegmentPlanDTO {
  if (mode === 4) return normalizeLayeredRewindSegment(segment, roll)
  const layoutItems = stripLayers(segment.layoutItems ?? defaultRewindSegment(roll).layoutItems)
  if (mode === 2 || mode === 3) {
    const layers = segment.layoutItems
      ?.filter((item) => item.itemType !== 'TRIM')
      .flatMap((item) => item.layers ?? []) ?? []
    const outDiameters = layers.map((layer) => layer.outDiameter)
      .filter((value): value is number => Number(value) > 0)
    const diameterSegment = {
      ...segment,
      targetDiameter: segment.targetDiameter
        ?? (outDiameters.length ? Math.max(...outDiameters) : undefined),
      finishCoreDiameter: segment.finishCoreDiameter
        ?? layers.find((layer) => Number(layer.coreDiameter) > 0)?.coreDiameter,
    }
    if (mode === 2) return normalizeDiameterSegment(diameterSegment, roll)
    return { ...diameterSegment, layoutItems }
  }
  return {
    ...segment,
    targetDiameter: undefined,
    finishCoreDiameter: undefined,
    layoutItems,
  }
}

function normalizeDiameterSegment(
  segment: RewindSegmentPlanDTO,
  roll: RollDraft,
): RewindSegmentPlanDTO {
  const source = segment.layoutItems?.find((item) => item.itemType !== 'TRIM')
  return {
    ...segment,
    layoutItems: [{
      width: effectiveRollWidth(roll),
      quantity: 1,
      itemType: 'FINISH',
      customerPaperName: source?.customerPaperName,
      customerGramWeight: source?.customerGramWeight,
      customerFinishWidth: source?.customerFinishWidth,
      customerSpecOverrideReason: source?.customerSpecOverrideReason,
    }],
  }
}

function stripLayers(items: RewindLayoutItemPlanDTO[] | undefined) {
  return items?.map(({ layers: _layers, ...item }) => item)
}

function normalizeSegmentRatios(segments: RewindSegmentPlanDTO[]) {
  if (segments.length === 1) return [{ ...segments[0], segmentRatio: 1 }]
  return segments.map((segment) => ({
    ...segment,
    segmentRatio: positiveRatio(segment),
  }))
}

function positiveRatio(segment: RewindSegmentPlanDTO) {
  const ratio = Number(segment.segmentRatio ?? 1)
  return Number.isFinite(ratio) && ratio > 0 ? ratio : 1
}
