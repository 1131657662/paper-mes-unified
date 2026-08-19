import { Button, InputNumber, Select, Space, Tag, Typography } from 'antd'
import TooltipText from '../../../components/biz/TooltipText'
import type { RewindSegmentPlanDTO, RewindSourcePlanDTO } from '../../../types/processOrder'
import { formatOptionalKg } from '../../../utils/numberFormatters'
import {
  consumptionSources,
  fullConsumptionSources,
  segmentConsumedWeight,
  sourceCompositionRatio,
  sourceConsumptionValue,
  sourceUsageRows,
} from '../rewindConsumptionUtils'
import { labelForSource, patchSource, sameSpecSourceIds, sourceOptionsFromRolls } from '../rewindSourceUtils'
import type { RollDraft } from '../types'

interface SourceEditorProps {
  segment: RewindSegmentPlanDTO
  roll: RollDraft
  rolls: RollDraft[]
  sourceOptions: ReturnType<typeof sourceOptionsFromRolls>
  onChange: (segment: RewindSegmentPlanDTO) => void
}

interface UsageSummaryProps {
  segments: RewindSegmentPlanDTO[]
  sourceOptions: ReturnType<typeof sourceOptionsFromRolls>
}

export function RewindSourceUsageSummary({ segments, sourceOptions }: UsageSummaryProps) {
  const rows = sourceUsageRows(segments, sourceOptions)
  if (!rows.length) return null

  return (
    <div className="rewind-source-summary">
      <Typography.Text strong>来源母卷消耗汇总</Typography.Text>
      <Space wrap style={{ marginTop: 8, display: 'flex' }}>
        {rows.map((row) => (
            <Tag key={row.originalUuid} color={tagColor(row.status)}>
            {row.label}：已用 {row.consumeRatio}% / 剩余 {row.remainingRatio}% / {formatOptionalKg(row.consumeWeight)}
          </Tag>
        ))}
      </Space>
    </div>
  )
}

export function RewindSourceEditor({ segment, roll, rolls, sourceOptions, onChange }: SourceEditorProps) {
  const sources = segment.sources ?? []
  const selectedIds = sources.map((source) => source.originalUuid).filter(Boolean) as string[]
  const sameSpecIds = sameSpecSourceIds(roll, rolls)
  const updateSources = (next: RewindSourcePlanDTO[]) => onChange({ ...segment, sources: next })

  return (
    <Space direction="vertical" style={{ width: '100%', marginTop: 12 }}>
      <Space wrap>
        <Tag color="blue">本段消耗：{formatOptionalKg(segmentConsumedWeight(sources, sourceOptions))}</Tag>
        <Typography.Text type="secondary">填写每卷本次实际使用比例；完整合并通常填写 100%，系统再按实际重量换算来源组成。</Typography.Text>
      </Space>
      <Select
        mode="multiple"
        value={selectedIds}
        options={sourceOptions}
        placeholder="选择来源母卷"
        maxTagCount="responsive"
        style={{ width: '100%' }}
        onChange={(values) => updateSources(consumptionSources(values, sources))}
      />
      <Space wrap>
        <Button size="small" onClick={() => updateSources(fullConsumptionSources(selectedIds))}>每卷用满</Button>
        <Button size="small" disabled={!sameSpecIds.length} onClick={() => updateSources(fullConsumptionSources(sameSpecIds))}>使用同规格母卷</Button>
      </Space>
      {sources.map((source, index) => (
        <div className="rewind-source-row" key={source.originalUuid ?? index}>
          <Tag>{index + 1}</Tag>
          <TooltipText className="rewind-source-editor__source-label" value={labelForSource(source, sourceOptions)} />
          <label className="rewind-field rewind-source-row__consume">
            <span className="rewind-field__label">本次使用比例</span>
            <InputNumber aria-label={`来源母卷 ${index + 1} 本次使用比例`} suffix="%"
              min={0.01} max={100} value={sourceConsumptionValue(source)}
              onChange={(value) => updateSources(patchSource(sources, index, { consumeRatio: value ?? 0 }))} />
          </label>
          <Tag color="geekblue">本段组成 {sourceCompositionRatio(source, sources, sourceOptions)}%</Tag>
        </div>
      ))}
    </Space>
  )
}

function tagColor(status: 'ok' | 'warning' | 'error'): string {
  if (status === 'ok') return 'success'
  if (status === 'error') return 'error'
  return 'warning'
}
