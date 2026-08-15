import { Button, message, Segmented, Select } from 'antd'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import MesTooltip from '../../../components/biz/MesTooltip'
import type { ProcessPlanDTO, RewindSegmentPlanDTO } from '../../../types/processOrder'
import { DEFAULT_WIDTH_DIFFERENCE_POLICY, WIDTH_DIFFERENCE_POLICY_OPTIONS } from '../../../constants/processOrder'
import { sourceOptionsFromRolls } from '../rewindSourceUtils'
import { appendRemainingTrim, rewindWidthPolicy } from '../rewindWidthUsage'
import {
  defaultRewindSegment,
  effectiveRollWidth,
  sameSpecRewindError,
  normalizeRewindPlan,
  planWithRewindMode,
} from '../rewindLayerPlanUtils'
import type { RollDraft } from '../types'
import RewindLayoutEditor from './RewindLayoutEditor'
import RewindSegmentFields from './RewindSegmentFields'
import { RewindSourceEditor, RewindSourceUsageSummary } from './RewindSourceEditor'
import RewindWidthSummary from './RewindWidthSummary'
import './CreateOrderEditors.css'

interface Props {
  plan: ProcessPlanDTO
  roll: RollDraft
  rolls: RollDraft[]
  onChange: (plan: ProcessPlanDTO) => void
}

const modeOptions = [
  { label: '改门幅', value: 1 }, { label: '改直径', value: 2 },
  { label: '门幅+直径', value: 3 }, { label: '内外层分层', value: 4 },
  { label: '多母卷合并', value: 5 }, { label: '同规格复卷', value: 6 },
]

export default function RewindPlanEditor({ plan, roll, rolls, onChange }: Props) {
  const mode = plan.rewindMode ?? 2
  const normalized = normalizeRewindPlan({ ...plan, rewindMode: mode }, roll)
  const segments = normalized.segments ?? [defaultRewindSegment(roll)]
  const sourceWidth = effectiveRollWidth(roll)
  const sourceOptions = sourceOptionsFromRolls(rolls)
  const widthPolicy = plan.widthDifferencePolicy ?? DEFAULT_WIDTH_DIFFERENCE_POLICY
  const widthPolicyEnabled = rewindWidthPolicy(mode).enabled
  const updateSegments = (next: RewindSegmentPlanDTO[]) => {
    onChange(normalizeRewindPlan({ ...plan, rewindMode: mode, segments: next }, roll))
  }
  const updateWidthPolicy = (value: string | number) => {
    if (value !== 'REMAINDER' && value !== 'ALLOCATE' && value !== 'LOSS') return
    onChange({ ...normalized, widthDifferencePolicy: value })
  }
  const changeMode = (nextMode: number) => {
    const error = nextMode === 6 ? sameSpecRewindError(roll) : undefined
    if (error) {
      message.error(error)
      return
    }
    onChange(planWithRewindMode(plan, roll, nextMode))
  }

  return (
    <div className="rewind-editor">
      <label className="rewind-mode-field">
        <span>复卷类型</span>
        <Select aria-label="复卷模式" value={mode} options={modeOptions}
          onChange={changeMode} />
      </label>
      {widthPolicyEnabled && (
        <Segmented aria-label="复卷门幅差额处理" value={widthPolicy}
          options={WIDTH_DIFFERENCE_POLICY_OPTIONS}
          onChange={updateWidthPolicy} />
      )}
      {mode === 5 && <RewindSourceUsageSummary segments={segments} sourceOptions={sourceOptions} />}
      {segments.map((segment, index) => <SegmentSection key={index}
        index={index} mode={mode} roll={roll} rolls={rolls} segment={segment} sourceWidth={sourceWidth}
        widthDifferencePolicy={widthPolicy}
        segments={segments} sourceOptions={sourceOptions}
        onChange={(next) => updateSegments(patchSegment(segments, index, next))}
        onDelete={segments.length <= 1 || mode === 6 ? undefined
          : () => updateSegments(segments.filter((_, itemIndex) => itemIndex !== index))} />)}
      {mode !== 6 && <Button className="rewind-editor__add-segment" icon={<PlusOutlined />}
        onClick={() => updateSegments([...segments, defaultRewindSegment(roll, segments.length + 1)])}>
        添加分段
      </Button>}
    </div>
  )
}

function SegmentSection(props: SegmentProps) {
  const { index, mode, roll, rolls, segment, segments, sourceOptions, widthDifferencePolicy, onChange, onDelete } = props
  return <section className="rewind-segment">
    <div className="rewind-segment__header">
      <strong>分段 {index + 1}</strong>
      {onDelete && <MesTooltip title="删除分段"><Button danger aria-label="删除复卷分段"
        size="small" icon={<DeleteOutlined />} onClick={onDelete} /></MesTooltip>}
    </div>
    <RewindSegmentFields index={index} mode={mode} roll={roll} segment={segment}
      segments={segments} onChange={onChange} />
    {mode !== 6 && <RewindWidthSummary mode={mode} originalWidth={props.sourceWidth}
      segment={segment} widthDifferencePolicy={widthDifferencePolicy}
      onFillTrim={() => onChange(appendRemainingTrim(segment, props.sourceWidth))} />}
    {mode !== 6 && <RewindLayoutEditor mode={mode} roll={roll} segment={segment} onChange={onChange} />}
    {mode === 5 && <RewindSourceEditor segment={segment} roll={roll} rolls={rolls}
      sourceOptions={sourceOptions} onChange={onChange} />}
  </section>
}

function patchSegment(segments: RewindSegmentPlanDTO[], index: number, next: RewindSegmentPlanDTO) {
  return segments.map((segment, itemIndex) => itemIndex === index ? next : segment)
}

interface SegmentProps {
  index: number
  mode: number
  roll: RollDraft
  rolls: RollDraft[]
  segment: RewindSegmentPlanDTO
  sourceWidth: number
  segments: RewindSegmentPlanDTO[]
  sourceOptions: ReturnType<typeof sourceOptionsFromRolls>
  widthDifferencePolicy: NonNullable<ProcessPlanDTO['widthDifferencePolicy']>
  onChange: (segment: RewindSegmentPlanDTO) => void
  onDelete?: () => void
}
