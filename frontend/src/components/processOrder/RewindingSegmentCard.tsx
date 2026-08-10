import { Button, InputNumber, Select, Space, Tag } from 'antd'
import { DeleteOutlined } from '@ant-design/icons'
import MesTooltip from '../biz/MesTooltip'
import RewindingSegmentLayout from './RewindingSegmentLayout'
import RewindingSegmentSources, { type SourceRollOption } from './RewindingSegmentSources'
import type { LayoutItemForm, SegmentForm } from './rewindingConfigModel'

export interface RewindingSegmentActions {
  onAddLayoutItem: (segmentKey: string, itemType: LayoutItemForm['itemType']) => void
  onEqualizeSources: (segmentKey: string) => void
  onLayoutItemChange: (segmentKey: string, itemKey: string, patch: Partial<LayoutItemForm>) => void
  onRemoveLayoutItem: (segmentKey: string, itemKey: string) => void
  onRemoveSegment: (segmentKey: string) => void
  onSegmentChange: (segmentKey: string, patch: Partial<SegmentForm>) => void
  onSegmentSourcesChange: (segmentKey: string, sourceUuids: string[]) => void
  onSourceRatioChange: (segmentKey: string, originalUuid: string, shareRatio: number) => void
}

interface SegmentContext {
  index: number
  originalWidth?: number
  rewindMode: number
  segmentCount: number
  sourceRollOptions: SourceRollOption[]
}

interface Props {
  actions: RewindingSegmentActions
  context: SegmentContext
  segment: SegmentForm
}

const CORE_DIAMETER_OPTIONS = [3, 4, 6, 12]

export default function RewindingSegmentCard({ actions, context, segment }: Props) {
  const disabled = context.rewindMode === 6
  return (
    <div style={{ border: '1px solid #f0f0f0', borderRadius: 6, padding: 12 }}>
      <SegmentHeader actions={actions} context={context} disabled={disabled} segment={segment} />
      {context.rewindMode === 5 && (
        <RewindingSegmentSources
          index={context.index}
          options={context.sourceRollOptions}
          segment={segment}
          onEqualize={() => actions.onEqualizeSources(segment.key)}
          onSourcesChange={(values) => actions.onSegmentSourcesChange(segment.key, values)}
          onRatioChange={(sourceUuid, ratio) => actions.onSourceRatioChange(segment.key, sourceUuid, ratio)}
        />
      )}
      <RewindingSegmentLayout
        actions={{
          onAdd: (itemType) => actions.onAddLayoutItem(segment.key, itemType),
          onChange: (itemKey, patch) => actions.onLayoutItemChange(segment.key, itemKey, patch),
          onRemove: (itemKey) => actions.onRemoveLayoutItem(segment.key, itemKey),
        }}
        disabled={disabled}
        index={context.index}
        originalWidth={context.originalWidth}
        segment={segment}
      />
    </div>
  )
}

interface HeaderProps extends Props {
  disabled: boolean
}

function SegmentHeader({ actions, context, disabled, segment }: HeaderProps) {
  return (
    <Space wrap style={{ marginBottom: 12 }}>
      <Tag color="blue">分段 {context.index + 1}</Tag>
      <SegmentRatioField actions={actions} context={context} segment={segment} />
      <InputNumber
        aria-label={`分段 ${context.index + 1} 成品直径上限`}
        min={0}
        value={segment.targetDiameter}
        onChange={(targetDiameter) => actions.onSegmentChange(segment.key, { targetDiameter: targetDiameter ?? undefined })}
        addonBefore="成品直径 ≤"
        suffix="cm"
        placeholder="不限"
        disabled={context.rewindMode === 1 || disabled}
      />
      <Select
        aria-label={`分段 ${context.index + 1} 成品纸芯`}
        value={segment.finishCoreDiameter ?? 3}
        onChange={(finishCoreDiameter) => actions.onSegmentChange(segment.key, { finishCoreDiameter })}
        style={{ width: 100 }}
        disabled={context.rewindMode === 1 || disabled}
        options={CORE_DIAMETER_OPTIONS.map((value) => ({ value, label: `纸芯 ${value}"` }))}
      />
      <InputNumber
        aria-label={`分段 ${context.index + 1} 重复次数`}
        min={1}
        value={segment.repeatCount}
        onChange={(repeatCount) => actions.onSegmentChange(segment.key, { repeatCount: repeatCount ?? 1 })}
        addonBefore="重复"
        suffix="次"
        disabled={disabled}
      />
      <Button
        danger
        aria-label={`删除复卷分段 ${context.index + 1}`}
        icon={<DeleteOutlined />}
        onClick={() => actions.onRemoveSegment(segment.key)}
        disabled={disabled}
      />
    </Space>
  )
}

function SegmentRatioField({ actions, context, segment }: Props) {
  if (context.segmentCount === 1) return <Tag color="default">单分段 100%</Tag>
  return (
    <MesTooltip title="该分段占母卷总直径/重量的比例，所有分段合计应为100%">
      <InputNumber
        aria-label={`分段 ${context.index + 1} 占比`}
        min={0}
        max={100}
        precision={0}
        value={segment.segmentRatio}
        onChange={(segmentRatio) => actions.onSegmentChange(segment.key, { segmentRatio: segmentRatio ?? 0 })}
        addonBefore="分段占比"
        suffix="%"
      />
    </MesTooltip>
  )
}
