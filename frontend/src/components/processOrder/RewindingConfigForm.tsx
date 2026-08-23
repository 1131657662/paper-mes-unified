import { useState } from 'react'
import { message } from 'antd'
import type { FinishConfigSaveDTO, OriginalRoll } from '../../types/processOrder'
import { formatOptionalTonFromKg } from '../../utils/numberFormatters'
import { isRollWeightKnown, rollTotalWeight } from '../../features/processOrderDetail/routeConfigSource'
import RewindingOnSiteFields from './RewindingOnSiteFields'
import RewindingStandardFields from './RewindingStandardFields'
import {
  buildDefaultSegments,
  buildInitialSegments,
  buildSameSpecSegments,
  defaultLayoutItem,
  defaultSegment,
  equalizeSourceRatios,
  sameSpecRewindError,
  toFinishSpecs,
  toPreviewDto,
  type LayoutItemForm,
  type SegmentForm,
} from './rewindingConfigModel'
import { useRewindingPlanPreview } from './useRewindingPlanPreview'
import {
  toOnSiteRewindingConfig,
  toRewindingConfig,
  type RewindingConfigValues,
} from './rewindingConfigSerializer'

interface Props {
  orderUuid: string
  roll: OriginalRoll
  originalRolls: OriginalRoll[]
  processMode: number
  config?: FinishConfigSaveDTO
  onChange: (config: FinishConfigSaveDTO) => void
}

export default function RewindingConfigForm(props: Props) {
  const { config, onChange, orderUuid, originalRolls, processMode, roll } = props
  const [rewindMode, setRewindMode] = useState(config?.rewindMode ?? 2)
  const [segments, setSegments] = useState<SegmentForm[]>(buildInitialSegments(roll, config))
  const [unitPrice, setUnitPrice] = useState<number | undefined>(config?.unitPrice)
  const [spareCount, setSpareCount] = useState(config?.spareCount ?? 0)
  const isStandardMode = processMode === 1
  const previewPlan = toPreviewDto(rewindMode, spareCount, segments)
  const { preview, previewing } = useRewindingPlanPreview({
    enabled: isStandardMode,
    orderUuid,
    rollUuid: roll.uuid,
    plan: previewPlan,
    onPreview: (nextPreview) => onChange(toRewindingConfig({
      nextPreview,
      nextRewindMode: rewindMode,
      nextUnitPrice: unitPrice,
      nextSpareCount: spareCount,
      nextSegments: segments,
      processMode,
    })),
  })

  const emitConfig = (options: Partial<RewindingConfigValues> = {}) => {
    onChange(toRewindingConfig({
      nextPreview: preview,
      nextRewindMode: rewindMode,
      nextUnitPrice: unitPrice,
      nextSpareCount: spareCount,
      nextSegments: segments,
      processMode,
      ...options,
    }))
  }
  const updateSegments = (nextSegments: SegmentForm[]) => {
    setSegments(nextSegments)
    emitConfig({ nextSegments })
  }
  const updateSegment = (key: string, patch: Partial<SegmentForm>) => {
    updateSegments(segments.map((segment) => (segment.key === key ? { ...segment, ...patch } : segment)))
  }
  const updateLayoutItem = (segmentKey: string, itemKey: string, patch: Partial<LayoutItemForm>) => {
    updateSegments(segments.map((segment) => segment.key === segmentKey ? {
      ...segment,
      layoutItems: segment.layoutItems.map((item) => item.key === itemKey ? { ...item, ...patch } : item),
    } : segment))
  }
  const updateSegmentSources = (segmentKey: string, sourceUuids: string[]) => {
    updateSegments(segments.map((segment) => segment.key === segmentKey
      ? segmentWithSources(segment, sourceUuids)
      : segment))
  }
  const updateSourceRatio = (segmentKey: string, originalUuid: string, shareRatio: number) => {
    updateSegments(segments.map((segment) => segment.key === segmentKey ? {
      ...segment,
      sources: segment.sources.map((source) => source.originalUuid === originalUuid
        ? { ...source, shareRatio }
        : source),
    } : segment))
  }
  const equalizeSources = (segmentKey: string) => {
    updateSegments(segments.map((segment) => segment.key === segmentKey
      ? { ...segment, sources: equalizeSourceRatios(segment.sources) }
      : segment))
  }
  const addSegment = () => updateSegments([
    ...segments,
    defaultSegment(segments.length + 1, roll.originalWidth ?? 1000, roll.uuid, rewindMode),
  ])
  const removeSegment = (key: string) => {
    if (segments.length <= 1) return void message.warning('至少保留一个分段')
    updateSegments(segments.filter((segment) => segment.key !== key))
  }
  const addLayoutItem = (segmentKey: string, itemType: LayoutItemForm['itemType']) => {
    updateSegments(segments.map((segment) => segment.key === segmentKey ? {
      ...segment,
      layoutItems: [...segment.layoutItems, { ...defaultLayoutItem(100), itemType }],
    } : segment))
  }
  const removeLayoutItem = (segmentKey: string, itemKey: string) => {
    updateSegments(segments.map((segment) => {
      if (segment.key !== segmentKey) return segment
      if (segment.layoutItems.length <= 1) {
        message.warning('每个分段至少保留一个门幅项')
        return segment
      }
      return { ...segment, layoutItems: segment.layoutItems.filter((item) => item.key !== itemKey) }
    }))
  }
  const changeRewindMode = (nextRewindMode: number) => {
    const error = nextRewindMode === 6 ? sameSpecRewindError(roll) : undefined
    if (error) {
      message.error(error)
      return
    }
    const nextSegments = nextRewindMode === 6
      ? buildSameSpecSegments(roll)
      : nextRewindMode === 2
        ? buildDefaultSegments(roll.originalWidth ?? 1000, roll.uuid, nextRewindMode)
        : segments
    setRewindMode(nextRewindMode)
    setSegments(nextSegments)
    emitConfig({ nextRewindMode, nextSegments })
  }
  const changeUnitPrice = (nextUnitPrice: number) => {
    setUnitPrice(nextUnitPrice)
    emitConfig({ nextUnitPrice })
  }
  const changeSpareCount = (nextSpareCount: number) => {
    setSpareCount(nextSpareCount)
    emitConfig({ nextSpareCount })
  }

  if (processMode === 2) {
    const totalFinishCount = preview?.finishCount ?? toFinishSpecs(null, segments).length
    return (
      <RewindingOnSiteFields
        value={{ processMode, rewindMode, spareCount, totalFinishCount, unitPrice }}
        onChange={(count) => onChange(toOnSiteRewindingConfig({ count, processMode, rewindMode, spareCount, unitPrice }))}
      />
    )
  }
  if (!isStandardMode) return null
  return (
    <RewindingStandardFields
      actions={{
        onAddLayoutItem: addLayoutItem,
        onAddSegment: addSegment,
        onEqualizeSources: equalizeSources,
        onLayoutItemChange: updateLayoutItem,
        onModeChange: changeRewindMode,
        onRemoveLayoutItem: removeLayoutItem,
        onRemoveSegment: removeSegment,
        onSegmentChange: updateSegment,
        onSegmentSourcesChange: updateSegmentSources,
        onSourceRatioChange: updateSourceRatio,
        onSpareCountChange: changeSpareCount,
        onUnitPriceChange: changeUnitPrice,
      }}
      roll={roll}
      sourceRollOptions={originalRolls.map((sourceRoll, index) => ({
        label: `原纸${index + 1} ${sourceRoll.rollNo || sourceRoll.paperName || ''}`,
        value: sourceRoll.uuid,
      }))}
      value={{
        preview,
        previewSegments: previewPlan.segments ?? [],
        previewing,
        rewindMode,
        segments,
        spareCount,
        tonnage: sourceTonnage(roll),
        unitPrice,
      }}
    />
  )
}

function sourceTonnage(roll: OriginalRoll) {
  if (!isRollWeightKnown(roll)) return '待称重'
  return formatOptionalTonFromKg(rollTotalWeight(roll))
}

function segmentWithSources(segment: SegmentForm, sourceUuids: string[]): SegmentForm {
  const keptSources = segment.sources.filter((source) => sourceUuids.includes(source.originalUuid))
  const addedSources = sourceUuids
    .filter((sourceUuid) => !keptSources.some((source) => source.originalUuid === sourceUuid))
    .map((originalUuid) => ({ originalUuid, shareRatio: 0 }))
  return { ...segment, sources: [...keptSources, ...addedSources] }
}
