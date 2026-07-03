import { Button, Card, InputNumber, Select, Space } from 'antd'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import MesTooltip from '../../../components/biz/MesTooltip'
import type {
  ProcessPlanDTO,
  RewindLayoutItemPlanDTO,
  RewindSegmentPlanDTO,
} from '../../../types/processOrder'
import { sourceOptionsFromRolls } from '../rewindSourceUtils'
import { appendRemainingTrim } from '../rewindWidthUsage'
import type { RollDraft } from '../types'
import RewindWidthSummary from './RewindWidthSummary'
import { RewindSourceEditor, RewindSourceUsageSummary } from './RewindSourceEditor'
import RewindLayerEditor from './RewindLayerEditor'
import './CreateOrderEditors.css'

interface Props {
  plan: ProcessPlanDTO
  roll: RollDraft
  rolls: RollDraft[]
  onChange: (plan: ProcessPlanDTO) => void
}

const modeOptions = [
  { label: '改门幅', value: 1 },
  { label: '改直径', value: 2 },
  { label: '门幅+直径', value: 3 },
  { label: '内外层分层', value: 4 },
  { label: '多母卷合并', value: 5 },
]

export default function RewindPlanEditor({ plan, roll, rolls, onChange }: Props) {
  const segments = plan.segments?.length ? plan.segments : [defaultSegment(roll)]
  const sourceOptions = sourceOptionsFromRolls(rolls)
  const updateSegments = (next: RewindSegmentPlanDTO[]) => onChange({ ...plan, segments: next })

  return (
    <Space className="create-editor-stack" direction="vertical" size={12}>
      <Select
        value={plan.rewindMode ?? 2}
        options={modeOptions}
        className="create-editor-mode-select"
        onChange={(value) => onChange(planWithMode(plan, roll, value))}
      />
      {(plan.rewindMode ?? 2) === 5 && <RewindSourceUsageSummary segments={segments} sourceOptions={sourceOptions} />}
      {segments.map((segment, index) => (
        <SegmentCard
          key={index}
          index={index}
          mode={plan.rewindMode ?? 2}
          roll={roll}
          rolls={rolls}
          segment={segment}
          sourceOptions={sourceOptions}
          onChange={(next) => updateSegments(patchSegment(segments, index, next))}
          onDelete={() => updateSegments(segments.filter((_, itemIndex) => itemIndex !== index))}
        />
      ))}
      <Button icon={<PlusOutlined />} onClick={() => updateSegments([...segments, defaultSegment(roll, segments.length + 1)])}>
        添加分段
      </Button>
    </Space>
  )
}

function SegmentCard({ index, mode, roll, rolls, segment, sourceOptions, onChange, onDelete }: SegmentProps) {
  return (
    <Card
      size="small"
      title={`分段 ${index + 1}`}
      extra={(
        <MesTooltip title="删除分段">
          <Button danger aria-label="删除复卷分段" size="small" icon={<DeleteOutlined />} onClick={onDelete} />
        </MesTooltip>
      )}
    >
      <Space wrap className="create-editor-row-gap">
        <InputNumber addonBefore="比例" min={0.01} value={segment.segmentRatio ?? 1} onChange={(value) => onChange({ ...segment, segmentRatio: value ?? 1 })} />
        <InputNumber addonBefore="直径" min={0} value={segment.targetDiameter} onChange={(value) => onChange({ ...segment, targetDiameter: value ?? undefined })} />
        <InputNumber addonBefore="纸芯" min={0} value={segment.finishCoreDiameter} onChange={(value) => onChange({ ...segment, finishCoreDiameter: value ?? undefined })} />
        <InputNumber addonBefore="重复" min={1} value={segment.repeatCount ?? 1} onChange={(value) => onChange({ ...segment, repeatCount: value ?? 1 })} />
      </Space>
      <RewindWidthSummary
        mode={mode}
        originalWidth={roll.originalWidth}
        segment={segment}
        onFillTrim={() => onChange(appendRemainingTrim(segment, roll.originalWidth))}
      />
      {mode !== 2 && <LayoutItemsEditor mode={mode} segment={segment} onChange={onChange} />}
      {mode === 5 && <RewindSourceEditor segment={segment} roll={roll} rolls={rolls} sourceOptions={sourceOptions} onChange={onChange} />}
    </Card>
  )
}

interface SegmentProps {
  index: number
  mode: number
  roll: RollDraft
  rolls: RollDraft[]
  segment: RewindSegmentPlanDTO
  sourceOptions: ReturnType<typeof sourceOptionsFromRolls>
  onChange: (segment: RewindSegmentPlanDTO) => void
  onDelete: () => void
}

function LayoutItemsEditor({ mode, segment, onChange }: LayoutItemsEditorProps) {
  const items = segment.layoutItems ?? []
  const update = (next: RewindLayoutItemPlanDTO[]) => onChange({ ...segment, layoutItems: next })
  return (
    <Space className="create-editor-stack" direction="vertical">
      {items.map((item, index) => (
        <Space key={index} className="create-editor-stack" direction="vertical">
          <Space wrap>
            <Select value={item.itemType ?? 'FINISH'} className="create-editor-kind-select" options={[{ label: '成品', value: 'FINISH' }, { label: '修边', value: 'TRIM' }]} onChange={(value) => update(patchItem(items, index, { itemType: value }))} />
            <InputNumber addonBefore="门幅" min={1} value={item.width} onChange={(value) => update(patchItem(items, index, { width: value ?? 1 }))} />
            <InputNumber addonBefore="数量" min={1} value={item.quantity ?? 1} onChange={(value) => update(patchItem(items, index, { quantity: value ?? 1 }))} />
            <MesTooltip title="删除排布">
              <Button
                danger
                aria-label="删除复卷排布"
                size="small"
                icon={<DeleteOutlined />}
                onClick={() => update(items.filter((_, itemIndex) => itemIndex !== index))}
              />
            </MesTooltip>
          </Space>
          {mode === 4 && (item.itemType ?? 'FINISH') === 'FINISH' && (
            <RewindLayerEditor
              item={item}
              defaultCoreDiameter={segment.finishCoreDiameter}
              defaultOutDiameter={segment.targetDiameter}
              onChange={(next) => update(patchItem(items, index, next))}
            />
          )}
        </Space>
      ))}
      <Button size="small" icon={<PlusOutlined />} onClick={() => update([...items, { width: 500, quantity: 1, itemType: 'FINISH' }])}>
        添加排布
      </Button>
    </Space>
  )
}

interface LayoutItemsEditorProps {
  mode: number
  segment: RewindSegmentPlanDTO
  onChange: (segment: RewindSegmentPlanDTO) => void
}

function defaultSegment(roll: RollDraft, sort = 1): RewindSegmentPlanDTO {
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

function planWithMode(plan: ProcessPlanDTO, roll: RollDraft, rewindMode: number): ProcessPlanDTO {
  if (rewindMode === 4) {
    return { ...plan, rewindMode, segments: layeredSegments(plan.segments?.length ? plan.segments : [defaultSegment(roll)], roll) }
  }
  if (rewindMode !== 2) return { ...plan, rewindMode }
  const segments = (plan.segments?.length ? plan.segments : [defaultSegment(roll)]).map((segment) => ({
    ...segment,
    layoutItems: [{ width: roll.originalWidth ?? 1, quantity: 1, itemType: 'FINISH' as const }],
  }))
  return { ...plan, rewindMode, segments }
}

function layeredSegments(segments: RewindSegmentPlanDTO[], roll: RollDraft) {
  return segments.map((segment) => ({
    ...segment,
    layoutItems: (segment.layoutItems?.length ? segment.layoutItems : defaultSegment(roll).layoutItems)?.map((item) => (
      item.itemType === 'TRIM' || item.layers?.length ? item : {
        ...item,
        layers: [{ outDiameter: segment.targetDiameter ?? roll.originalDiameter, coreDiameter: segment.finishCoreDiameter ?? roll.coreDiameter ?? 3 }],
      }
    )),
  }))
}

function patchSegment(segments: RewindSegmentPlanDTO[], index: number, next: RewindSegmentPlanDTO) {
  return segments.map((segment, itemIndex) => (itemIndex === index ? next : segment))
}

function patchItem(items: RewindLayoutItemPlanDTO[], index: number, patch: Partial<RewindLayoutItemPlanDTO>) {
  return items.map((item, itemIndex) => (itemIndex === index ? { ...item, ...patch } : item))
}
