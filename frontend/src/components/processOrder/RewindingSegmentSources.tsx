import { Button, InputNumber, Select, Space, Typography } from 'antd'
import TooltipText from '../biz/TooltipText'
import type { SegmentForm } from './rewindingConfigModel'

export interface SourceRollOption {
  label: string
  value: string
}

interface Props {
  index: number
  options: SourceRollOption[]
  segment: SegmentForm
  onEqualize: () => void
  onRatioChange: (originalUuid: string, shareRatio: number) => void
  onSourcesChange: (sourceUuids: string[]) => void
}

export default function RewindingSegmentSources(props: Props) {
  const totalRatio = props.segment.sources.reduce((sum, source) => sum + source.shareRatio, 0)
  return (
    <div style={{ marginBottom: 12, padding: 8, background: '#fafafa', borderRadius: 6 }}>
      <Space direction="vertical" style={{ width: '100%' }}>
        <Space wrap>
          <Typography.Text strong>来源母卷（每段分摊比例合计必须 = 100%）</Typography.Text>
          <Typography.Text type={Math.abs(totalRatio - 100) < 0.01 ? 'success' : 'danger'}>
            合计 {totalRatio.toFixed(2)}%
          </Typography.Text>
        </Space>
        <Select
          aria-label={`分段 ${props.index + 1} 来源母卷`}
          mode="multiple"
          value={props.segment.sources.map((source) => source.originalUuid)}
          options={props.options}
          onChange={props.onSourcesChange}
          placeholder="选择参与这一段接纸的母卷"
          style={{ width: '100%' }}
        />
        <SourceRatioFields {...props} />
      </Space>
    </div>
  )
}

function SourceRatioFields({ options, segment, index, onEqualize, onRatioChange }: Props) {
  return (
    <Space wrap>
      {segment.sources.map((source) => {
        const label = options.find((item) => item.value === source.originalUuid)?.label
        return (
          <Space key={source.originalUuid} size={4}>
            <TooltipText className="rewinding-config-form__source-label" value={label ?? source.originalUuid} />
            <InputNumber
              aria-label={`分段 ${index + 1} 来源母卷分摊比例`}
              min={0}
              max={100}
              precision={2}
              value={source.shareRatio}
              onChange={(value) => onRatioChange(source.originalUuid, value ?? 0)}
              suffix="%"
              style={{ width: 120 }}
            />
          </Space>
        )
      })}
      {segment.sources.length >= 2 && (
        <Button size="small" onClick={onEqualize}>
          自动均分
        </Button>
      )}
    </Space>
  )
}
