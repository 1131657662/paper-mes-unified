import type {
  FinishLayerDTO,
  ProcessPlanDTO,
  RewindLayoutItemPlanDTO,
  RewindSegmentPlanDTO,
} from '../../types/processOrder'
import type { RollDraft } from './types'

export function defaultRewindSegment(roll: RollDraft, sort = 1): RewindSegmentPlanDTO {
  return {
    segmentSort: sort,
    segmentRatio: 1,
    targetDiameter: roll.originalDiameter,
    finishCoreDiameter: roll.coreDiameter ?? 3,
    repeatCount: 1,
    sources: roll.uuid ? [{ originalUuid: roll.uuid, shareRatio: 100, consumeRatio: 100, sourceSort: 1 }] : [],
    layoutItems: [{ width: roll.originalWidth, quantity: 1, itemType: 'FINISH' }],
  }
}

export function normalizeLayeredRewindPlan(plan: ProcessPlanDTO, roll: RollDraft): ProcessPlanDTO {
  if (plan.rewindMode !== 4) return plan
  return { ...plan, segments: normalizeLayeredRewindSegments(plan.segments, roll) }
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
