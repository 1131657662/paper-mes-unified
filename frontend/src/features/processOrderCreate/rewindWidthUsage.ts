import type { RewindLayoutItemPlanDTO, RewindSegmentPlanDTO } from '../../types/processOrder'

export interface RewindWidthUsage {
  originalWidth: number
  finishWidth: number
  implicitTrimWidth: number
  trimWidth: number
  usedWidth: number
  remainingWidth: number
  finishCount: number
  trimCount: number
  usedPercent: number
}

export interface RewindWidthPolicy {
  enabled: boolean
  note: string
}

export function calcRewindWidthUsage(
  segment: RewindSegmentPlanDTO,
  originalWidth: number | undefined,
): RewindWidthUsage {
  const width = Number(originalWidth ?? 0)
  const items = segment.layoutItems ?? []
  const finishWidth = sumLayoutWidth(items, false)
  const trimWidth = sumLayoutWidth(items, true)
  const usedWidth = finishWidth + trimWidth
  const remainingWidth = width - usedWidth
  const implicitTrimWidth = trimWidth > 0 ? 0 : Math.max(0, width - finishWidth)

  return {
    originalWidth: width,
    finishWidth,
    implicitTrimWidth,
    trimWidth,
    usedWidth,
    remainingWidth,
    finishCount: countLayoutItems(items, false),
    trimCount: countLayoutItems(items, true),
    usedPercent: width > 0 ? Math.min(100, Math.round((usedWidth / width) * 100)) : 0,
  }
}

export function rewindWidthPolicy(mode: number): RewindWidthPolicy {
  if (mode === 2) {
    return { enabled: false, note: '改直径不做横向门幅排布，门幅默认沿用母卷门幅。' }
  }
  if (mode === 3) {
    return { enabled: true, note: '每个直径分段单独校验门幅，分段之间不横向相加。' }
  }
  if (mode === 4) {
    return { enabled: true, note: '内外层每段可有不同排布，每段分别和母卷门幅比较。' }
  }
  if (mode === 5) {
    return { enabled: true, note: '合并复卷按目标排布校验门幅，来源母卷负责重量与接纸关系。' }
  }
  return { enabled: true, note: '改门幅按本段成品和修边的横向宽度合计校验。' }
}

export function appendRemainingTrim(segment: RewindSegmentPlanDTO, originalWidth: number | undefined) {
  const usage = calcRewindWidthUsage(segment, originalWidth)
  if (usage.remainingWidth <= 0) return segment
  return {
    ...segment,
    layoutItems: [
      ...(segment.layoutItems ?? []),
      { width: usage.remainingWidth, quantity: 1, itemType: 'TRIM' as const },
    ],
  }
}

function sumLayoutWidth(items: RewindLayoutItemPlanDTO[], trim: boolean) {
  return items
    .filter((item) => isTrim(item) === trim)
    .reduce((sum, item) => sum + item.width * safeQuantity(item), 0)
}

function countLayoutItems(items: RewindLayoutItemPlanDTO[], trim: boolean) {
  return items.filter((item) => isTrim(item) === trim).reduce((sum, item) => sum + safeQuantity(item), 0)
}

function isTrim(item: RewindLayoutItemPlanDTO) {
  return item.itemType === 'TRIM'
}

function safeQuantity(item: RewindLayoutItemPlanDTO) {
  return item.quantity ?? 1
}
