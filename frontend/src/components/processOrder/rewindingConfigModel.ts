import type {
  FinishConfigSaveDTO,
  FinishConfigSpecDTO,
  FinishPreviewVO,
  OriginalRoll,
  RewindLayoutItemDTO,
  RewindPlanPreviewDTO,
  RewindSegmentDTO,
} from '../../types/processOrder'

export interface SegmentForm extends RewindSegmentDTO {
  key: string
  sources: SourceForm[]
  layoutItems: LayoutItemForm[]
}

export interface SourceForm {
  originalUuid: string
  shareRatio: number
}

export interface LayoutItemForm extends RewindLayoutItemDTO {
  key: string
  itemType: 'FINISH' | 'TRIM'
}

export function equalizeSourceRatios(sources: SourceForm[]): SourceForm[] {
  if (sources.length === 0) return []
  const average = Number((100 / sources.length).toFixed(2))
  const remainder = Number((100 - average * (sources.length - 1)).toFixed(2))
  return sources.map((source, index) => ({
    ...source,
    shareRatio: index === sources.length - 1 ? remainder : average,
  }))
}

const newKey = () => String(Date.now() + Math.random())

export const defaultLayoutItem = (width = 500): LayoutItemForm => ({
  key: newKey(),
  width,
  quantity: 1,
  itemType: 'FINISH',
})

export const defaultSegment = (
  sort = 1,
  originalWidth = 1000,
  sourceUuid?: string,
  rewindMode = 1,
): SegmentForm => ({
  key: newKey(),
  segmentSort: sort,
  segmentRatio: 100,
  targetDiameter: undefined,
  finishCoreDiameter: 3,
  repeatCount: 1,
  sources: sourceUuid ? [{ originalUuid: sourceUuid, shareRatio: 100 }] : [],
  layoutItems: [defaultLayoutItem(rewindMode === 2 ? originalWidth : Math.floor(originalWidth / 2) || 500)],
})

export function buildDefaultSegments(originalWidth: number, sourceUuid: string, rewindMode: number) {
  return [defaultSegment(1, originalWidth, sourceUuid, rewindMode)]
}

export function buildSameSpecSegments(roll: OriginalRoll): SegmentForm[] {
  return [{
    ...defaultSegment(1, roll.originalWidth ?? 1000, roll.uuid, 6),
    targetDiameter: toCm(roll.originalDiameter),
    finishCoreDiameter: roll.coreDiameter ?? 3,
    repeatCount: 1,
    layoutItems: [defaultLayoutItem(roll.originalWidth ?? 1000)],
  }]
}

export const toCm = (inch?: number) => (inch != null ? Math.round(inch * 2.54) : undefined)
export const toInch = (cm?: number) => (cm != null ? Math.round(cm / 2.54) : undefined)

export function buildSegmentFromDto(
  segment: RewindSegmentDTO,
  index: number,
  originalWidth: number,
  sourceUuid?: string,
  rewindMode = 1,
): SegmentForm {
  return {
    key: newKey(),
    segmentSort: segment.segmentSort ?? index + 1,
    segmentRatio: (segment.segmentRatio ?? 0) * 100,
    targetDiameter: toCm(segment.targetDiameter),
    finishCoreDiameter: segment.finishCoreDiameter ?? 3,
    repeatCount: segment.repeatCount ?? 1,
    sources: segment.sources?.length
      ? segment.sources.map((source) => ({ originalUuid: source.originalUuid ?? '', shareRatio: source.shareRatio ?? 0 }))
      : sourceUuid ? [{ originalUuid: sourceUuid, shareRatio: 100 }] : [],
    layoutItems: segment.layoutItems?.length
      ? segment.layoutItems.map((item) => ({
        key: newKey(),
        width: item.width,
        quantity: item.quantity ?? 1,
        itemType: item.itemType ?? 'FINISH',
        customerPaperName: item.customerPaperName,
        customerGramWeight: item.customerGramWeight,
        customerFinishWidth: item.customerFinishWidth,
        customerSpecOverrideReason: item.customerSpecOverrideReason,
        layers: item.layers?.map((layer) => ({ ...layer })),
      }))
      : [defaultLayoutItem(rewindMode === 2 ? originalWidth : Math.floor(originalWidth / 2) || 500)],
  }
}

export function buildInitialSegments(roll: OriginalRoll, config?: FinishConfigSaveDTO): SegmentForm[] {
  const originalWidth = roll.originalWidth ?? 1000
  const initialRewindMode = config?.rewindMode ?? 2
  if (config?.rewindSegments?.length) {
    return config.rewindSegments.map((segment, index) =>
      buildSegmentFromDto(segment, index, originalWidth, roll.uuid, initialRewindMode),
    )
  }
  return buildDefaultSegments(originalWidth, roll.uuid, initialRewindMode)
}

export function toPreviewDto(rewindMode: number, spareCount: number, segments: SegmentForm[]): RewindPlanPreviewDTO {
  return {
    rewindMode,
    spareCount,
    segments: segments.map(({ key: _key, layoutItems, sources, ...segment }) => ({
      ...segment,
      segmentRatio: segments.length === 1 ? 1 : (segment.segmentRatio ?? 0) / 100,
      targetDiameter: toInch(segment.targetDiameter),
      sources: sources.map(({ originalUuid, shareRatio }) => ({ originalUuid, shareRatio })),
      layoutItems: layoutItems.map(({ key: _itemKey, ...item }) => ({
        ...item,
        layers: rewindMode === 4 && item.itemType !== 'TRIM'
          ? item.layers?.length ? item.layers : [{ outDiameter: toInch(segment.targetDiameter), coreDiameter: segment.finishCoreDiameter }]
          : item.layers,
      })),
    })),
  }
}

export function toFinishSpecs(preview: FinishPreviewVO | null, segments: SegmentForm[]): FinishConfigSpecDTO[] {
  if (preview?.finishes?.length) {
    return preview.finishes.map((finish) => ({
      count: 1,
      finishWidth: finish.finishWidth,
      finishDiameter: finish.finishDiameter,
      finishCoreDiameter: finish.finishCoreDiameter,
      customerPaperName: finish.customerPaperName,
      customerGramWeight: finish.customerGramWeight,
      customerFinishWidth: finish.customerFinishWidth,
      customerSpecOverrideReason: finish.customerSpecOverrideReason,
      estimateWeight: finish.estimateWeight,
      layers: finish.layers,
    }))
  }

  return segments.flatMap((segment) => {
    const repeatCount = segment.repeatCount ?? 1
    return segment.layoutItems.flatMap((item) => {
      if (item.itemType !== 'FINISH') return []
      const quantity = item.quantity ?? 1
      return Array.from({ length: repeatCount * quantity }, () => ({
        count: 1,
        finishWidth: item.width,
        finishDiameter: toInch(segment.targetDiameter),
        finishCoreDiameter: segment.finishCoreDiameter,
        customerPaperName: item.customerPaperName,
        customerGramWeight: item.customerGramWeight,
        customerFinishWidth: item.customerFinishWidth,
        customerSpecOverrideReason: item.customerSpecOverrideReason,
        estimateWeight: 0,
      }))
    })
  })
}
