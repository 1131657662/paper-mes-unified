import { InputNumber, Typography } from 'antd'
import type { RewindSegmentPlanDTO } from '../../../types/processOrder'
import {
  formatGram,
  formatMm,
  formatStoredDiameter,
  formatStoredCoreDiameter,
  storedCoreDiameterUnit,
  storedDiameterUnit,
} from '../../../utils/numberFormatters'
import { effectiveRollWidth, segmentRatioPercent } from '../rewindLayerPlanUtils'
import type { RollDraft } from '../types'

interface Props {
  index: number
  mode: number
  roll: RollDraft
  segment: RewindSegmentPlanDTO
  segments: RewindSegmentPlanDTO[]
  onChange: (segment: RewindSegmentPlanDTO) => void
}

export default function RewindSegmentFields(props: Props) {
  if (props.mode === 6) return <SameSpecSummary roll={props.roll} />
  const showDiameter = props.mode === 2 || props.mode === 3
  return (
    <div className="rewind-field-grid">
      <RatioField {...props} />
      {showDiameter && <DiameterField {...props} field="targetDiameter" label="目标卷径" />}
      {showDiameter && <CoreDiameterField {...props} />}
      <NumberField {...props} field="repeatCount" label="重复次数" min={1} suffix="次" />
    </div>
  )
}

function RatioField({ index, segment, segments, onChange }: Props) {
  const percent = segmentRatioPercent(segment, segments)
  return (
    <label className="rewind-field">
      <span className="rewind-field__label">分配权重</span>
      {segments.length === 1
        ? <span className="rewind-field__readonly">100%</span>
        : <InputNumber aria-label={`分段 ${index + 1} 分配权重`} min={0.01}
          value={segment.segmentRatio ?? 1}
          onChange={(value) => onChange({ ...segment, segmentRatio: value ?? 1 })} />}
      {segments.length > 1 && <Typography.Text type="secondary">占比 {percent}%</Typography.Text>}
    </label>
  )
}

function DiameterField(props: Props & DiameterFieldProps) {
  const value = props.segment[props.field]
  return (
    <label className="rewind-field">
      <span className="rewind-field__label">{props.label}</span>
      <InputNumber aria-label={`分段 ${props.index + 1} ${props.label}`} min={1}
        suffix={storedDiameterUnit(value)} value={value}
        onChange={(next) => props.onChange({ ...props.segment, [props.field]: next ?? undefined })} />
    </label>
  )
}

function NumberField(props: Props & NumberFieldProps) {
  const value = props.segment[props.field]
  return (
    <label className="rewind-field">
      <span className="rewind-field__label">{props.label}</span>
      <InputNumber aria-label={`分段 ${props.index + 1} ${props.label}`} min={props.min}
        suffix={props.suffix} value={value}
        onChange={(next) => props.onChange({ ...props.segment, [props.field]: next ?? props.min })} />
    </label>
  )
}

function CoreDiameterField(props: Props) {
  const value = props.segment.finishCoreDiameter
  return <label className="rewind-field">
    <span className="rewind-field__label">成品纸芯</span>
    <InputNumber aria-label={`分段 ${props.index + 1} 成品纸芯`} min={1}
      suffix={storedCoreDiameterUnit(value)} value={value}
      onChange={(next) => props.onChange({ ...props.segment, finishCoreDiameter: next ?? undefined })} />
  </label>
}

function SameSpecSummary({ roll }: { roll: RollDraft }) {
  const items = [
    roll.paperName || '-', formatGram(roll.gramWeight), formatMm(effectiveRollWidth(roll)),
    `卷径 ${formatStoredDiameter(roll.originalDiameter)}`,
    `纸芯 ${formatStoredCoreDiameter(roll.coreDiameter)}`,
  ]
  return <div className="rewind-same-spec" aria-label="同规格成品规格">
    {items.map((item) => <span key={item}>{item}</span>)}
  </div>
}

interface DiameterFieldProps {
  field: 'targetDiameter' | 'finishCoreDiameter'
  label: string
}

interface NumberFieldProps {
  field: 'repeatCount'
  label: string
  min: number
  suffix: string
}
