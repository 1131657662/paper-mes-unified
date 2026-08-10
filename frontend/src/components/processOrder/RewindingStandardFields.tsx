import { Button, Col, Divider, InputNumber, Row, Select, Space, Tag, Typography } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import type { FinishPreviewVO, OriginalRoll, RewindSegmentDTO } from '../../types/processOrder'
import FinishConfigPlanPreviewPanel from './FinishConfigPlanPreviewPanel'
import RewindingSegmentCard, { type RewindingSegmentActions } from './RewindingSegmentCard'
import type { SourceRollOption } from './RewindingSegmentSources'
import type { SegmentForm } from './rewindingConfigModel'

export interface RewindingStandardValue {
  preview: FinishPreviewVO | null
  previewSegments: RewindSegmentDTO[]
  previewing: boolean
  rewindMode: number
  segments: SegmentForm[]
  spareCount: number
  tonnage: string
  unitPrice?: number
}

export interface RewindingStandardActions extends RewindingSegmentActions {
  onAddSegment: () => void
  onModeChange: (rewindMode: number) => void
  onSpareCountChange: (spareCount: number) => void
  onUnitPriceChange: (unitPrice: number) => void
}

interface Props {
  actions: RewindingStandardActions
  roll: OriginalRoll
  sourceRollOptions: SourceRollOption[]
  value: RewindingStandardValue
}

const REWIND_MODES = {
  1: '改门幅不变直径',
  2: '改直径不变门幅',
  3: '改门幅+改直径',
  4: '内外层分层',
  5: '多母卷合并复卷',
  6: '同规格复卷',
}

export default function RewindingStandardFields({ actions, roll, sourceRollOptions, value }: Props) {
  const disabled = value.rewindMode === 6
  return (
    <Row gutter={16} align="top">
      <Col span={14}>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Space wrap>
            <Typography.Text strong>复卷模式</Typography.Text>
            <Select
              aria-label="复卷模式"
              value={value.rewindMode}
              onChange={actions.onModeChange}
              style={{ width: 220 }}
              options={Object.entries(REWIND_MODES).map(([mode, label]) => ({ value: Number(mode), label }))}
            />
            <Button type="dashed" icon={<PlusOutlined />} onClick={actions.onAddSegment} disabled={disabled}>
              添加分段
            </Button>
            {value.previewing && <Tag color="processing">预览计算中…</Tag>}
          </Space>
          {value.segments.map((segment, index) => (
            <RewindingSegmentCard
              actions={actions}
              context={{
                index,
                originalWidth: roll.originalWidth,
                rewindMode: value.rewindMode,
                segmentCount: value.segments.length,
                sourceRollOptions,
              }}
              key={segment.key}
              segment={segment}
            />
          ))}
          <RewindingPricingFields actions={actions} value={value} />
        </Space>
      </Col>
      <Col span={10}>
        <div style={{ border: '1px solid #f0f0f0', borderRadius: 6, padding: 12, minHeight: 420 }}>
          <FinishConfigPlanPreviewPanel
            segments={value.previewSegments}
            originalWidth={roll.originalWidth}
            preview={value.preview}
            spareCount={value.spareCount}
            loading={value.previewing}
          />
        </div>
      </Col>
    </Row>
  )
}

function RewindingPricingFields({ actions, value }: Pick<Props, 'actions' | 'value'>) {
  return (
    <>
      <Divider style={{ margin: '8px 0' }} />
      <Space wrap>
        <InputNumber
          aria-label="复卷单价"
          min={0}
          precision={2}
          value={value.unitPrice}
          onChange={(unitPrice) => actions.onUnitPriceChange(unitPrice ?? 0)}
          addonBefore="复卷单价"
          suffix="元/吨"
        />
        <InputNumber
          aria-label="备用卷号数量"
          min={0}
          max={10}
          value={value.spareCount}
          onChange={(spareCount) => actions.onSpareCountChange(spareCount ?? 0)}
          addonBefore="备用卷号"
          suffix="个"
        />
        <Typography.Text type="secondary">母卷吨位：{value.tonnage}</Typography.Text>
      </Space>
    </>
  )
}
