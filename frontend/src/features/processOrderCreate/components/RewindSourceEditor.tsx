import { Button, InputNumber, Select, Space, Tag, Typography } from 'antd'
import TooltipText from '../../../components/biz/TooltipText'
import type { RewindSegmentPlanDTO, RewindSourcePlanDTO } from '../../../types/processOrder'
import { formatKg } from '../../../utils/numberFormatters'
import {
  consumptionSources,
  equalConsumptionSources,
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
    <div style={{ padding: 10, border: '1px solid #e5e7eb', borderRadius: 6, background: '#fafafa' }}>
      <Typography.Text strong>来源母卷消耗汇总</Typography.Text>
      <Space wrap style={{ marginTop: 8, display: 'flex' }}>
        {rows.map((row) => (
          <Tag key={row.originalUuid} color={tagColor(row.status)}>
            {row.label}：已用 {row.consumeRatio}% / 剩余 {row.remainingRatio}% / {formatKg(row.consumeWeight)}
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
        <Tag color="blue">本段消耗：{formatKg(segmentConsumedWeight(sources, sourceOptions))}</Tag>
        <Typography.Text type="secondary">输入每卷在整套合并方案中的消耗比例，后端按重量换算本段组成。</Typography.Text>
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
        <Button size="small" onClick={() => updateSources(equalConsumptionSources(selectedIds))}>平均消耗</Button>
        <Button size="small" disabled={!sameSpecIds.length} onClick={() => updateSources(fullConsumptionSources(sameSpecIds))}>使用同规格母卷</Button>
      </Space>
      {sources.map((source, index) => (
        <Space key={source.originalUuid ?? index} wrap>
          <Tag>{index + 1}</Tag>
          <TooltipText className="rewind-source-editor__source-label" value={labelForSource(source, sourceOptions)} />
          <InputNumber
            aria-label={`来源母卷 ${index + 1} 消耗比例`}
            addonBefore="消耗"
            suffix="%"
            min={0.01}
            max={100}
            value={sourceConsumptionValue(source)}
            onChange={(value) => updateSources(patchSource(sources, index, { consumeRatio: value ?? 0 }))}
          />
          <Tag color="geekblue">本段组成 {sourceCompositionRatio(source, sources, sourceOptions)}%</Tag>
        </Space>
      ))}
    </Space>
  )
}

function tagColor(status: 'ok' | 'warning' | 'error'): string {
  if (status === 'ok') return 'success'
  if (status === 'error') return 'error'
  return 'warning'
}
