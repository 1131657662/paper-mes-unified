import { Alert, Button, Descriptions, Empty, Space, Table, Tag, Typography } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { ReactNode } from 'react'
import TooltipText from '../../../components/biz/TooltipText'
import type {
  PlanPreviewVO,
  RewindFinishItemPreview,
  RewindSegmentPreview,
} from '../../../types/processOrder'
import { formatKgWithMaxDecimals, formatMm } from '../../../utils/numberFormatters'
import { createStableObjectRowKey } from '../../../utils/createStableObjectRowKey'
import type { RollWeightBalance } from '../weightBalanceModel'
import WeightBalanceStrip from './WeightBalanceStrip'
import './PlanPreviewPanel.css'
import './WeightBalanceStrip.css'

interface Props {
  preview?: PlanPreviewVO
  loading?: boolean
  onPreview?: () => void
  balance?: RollWeightBalance
}

const segmentColumns: ColumnsType<RewindSegmentPreview> = [
  { title: '段', dataIndex: 'segmentSort', width: 52, render: (value) => `#${value ?? '-'}` },
  { title: '比例', dataIndex: 'segmentRatio', width: 72, render: (value) => `${value ?? '-'}%` },
  { title: '目标直径', dataIndex: 'targetDiameter', width: 88, render: (value) => value ? formatMm(value) : '-' },
  { title: '重复', dataIndex: 'repeatCount', width: 58 },
  { title: '排布', dataIndex: 'layoutWidth', width: 84, render: (value) => value ? formatMm(value) : '-' },
  { title: '修边', dataIndex: 'trimWidth', width: 72, render: (value) => value ? formatMm(value) : '-' },
  { title: '摘要', dataIndex: 'summary', width: 180, render: textCell },
]

const finishColumns: ColumnsType<RewindFinishItemPreview> = [
  { title: '段', dataIndex: 'segmentSort', width: 52, render: (value) => value ? `#${value}` : '-' },
  { title: '门幅', dataIndex: 'finishWidth', width: 72, render: (value) => formatMm(value ?? 0) },
  { title: '直径', dataIndex: 'finishDiameter', width: 76, render: (value) => value ? formatMm(value) : '-' },
  { title: '纸芯', dataIndex: 'finishCoreDiameter', width: 72, render: (value) => value ? formatMm(value) : '-' },
  { title: '预估重', dataIndex: 'estimateWeight', width: 92, render: (value) => formatKgWithMaxDecimals(Number(value ?? 0), 2) },
  { title: '修边重', dataIndex: 'trimWeight', width: 92, render: (value) => formatKgWithMaxDecimals(Number(value ?? 0), 2) },
  { title: '来源', dataIndex: 'sourceSummary', width: 160, render: textCell },
]

const sawFinishColumns = finishColumns.filter((column) => !('dataIndex' in column) || column.dataIndex !== 'trimWeight')
const segmentRowKey = createStableObjectRowKey('preview-segment')
const finishRowKey = createStableObjectRowKey('preview-finish')

export default function PlanPreviewPanel({ preview, loading, onPreview, balance }: Props) {
  return (
    <div className="plan-preview-panel">
      <PreviewToolbar preview={preview} loading={loading} onPreview={onPreview} />
      {!preview ? <EmptyPreview /> : <PreviewContent preview={preview} balance={balance} />}
    </div>
  )
}

function PreviewToolbar({ preview, loading, onPreview }: Props) {
  return (
    <div className="plan-preview-panel__toolbar">
      <Space size={8} wrap>
        {preview && <Tag color={preview.ready ? 'success' : 'error'}>{preview.ready ? '可提交' : '需修正'}</Tag>}
        {loading && <Tag color="processing">预览中</Tag>}
      </Space>
      {onPreview && (
        <Button size="small" icon={<ReloadOutlined />} loading={loading} onClick={onPreview}>
          刷新预览
        </Button>
      )}
    </div>
  )
}

function EmptyPreview() {
  return (
    <div className="plan-preview-panel__empty">
      <Empty description="暂无预览结果" />
    </div>
  )
}

function PreviewContent({ preview, balance }: { preview: PlanPreviewVO; balance?: RollWeightBalance }) {
  const isRewind = preview.mainStepType === 2
  return (
    <div className="plan-preview-panel__content">
      {preview.errors?.length ? <Alert type="error" showIcon message={preview.errors.join('；')} /> : null}
      <Typography.Paragraph className="plan-preview-panel__summary">
        {preview.summary || '暂无摘要'}
      </Typography.Paragraph>
      {balance && <WeightBalanceStrip balance={balance} compact />}
      <PreviewStats preview={preview} />
      <PreviewTableSection
        title="排布数据"
        count={preview.segments?.length ?? 0}
        table={<SegmentTable preview={preview} isRewind={isRewind} />}
      />
      <PreviewTableSection
        title="成品数据"
        count={preview.finishes?.length ?? 0}
        table={<FinishTable preview={preview} isRewind={isRewind} />}
      />
    </div>
  )
}

function PreviewStats({ preview }: { preview: PlanPreviewVO }) {
  if (preview.widthDifferencePolicy) return <SawPreviewStats preview={preview} />
  return (
    <Descriptions size="small" column={1} bordered className="plan-preview-panel__stats">
      <Descriptions.Item label="成品">{preview.finishCount ?? 0}</Descriptions.Item>
      <Descriptions.Item label="修边">{preview.trimCount ?? 0}</Descriptions.Item>
      <Descriptions.Item label="备用">{preview.spareCount ?? 0}</Descriptions.Item>
      <Descriptions.Item label="成品重">{formatKgWithMaxDecimals(preview.totalEstimateWeight, 2)}</Descriptions.Item>
      <Descriptions.Item label="修边重">{formatKgWithMaxDecimals(preview.totalTrimWeight, 2)}</Descriptions.Item>
    </Descriptions>
  )
}

function SawPreviewStats({ preview }: { preview: PlanPreviewVO }) {
  const outcome = sawPolicyOutcome(preview)
  return (
    <Descriptions size="small" column={1} bordered className="plan-preview-panel__stats">
      <Descriptions.Item label="成品">
        {preview.finishCount ?? 0} 件 / {formatKgWithMaxDecimals(preview.totalEstimateWeight, 2)}
      </Descriptions.Item>
      <Descriptions.Item label="门幅差额">
        {formatMm(preview.widthDifference ?? 0)} / {formatKgWithMaxDecimals(sawDifferenceWeight(preview), 2)}
      </Descriptions.Item>
      <Descriptions.Item label="处理结果">
        <Tag color={outcome.color}>{outcome.label}</Tag>{' '}
        <Typography.Text type="secondary">{outcome.detail}</Typography.Text>
      </Descriptions.Item>
    </Descriptions>
  )
}

function PreviewTableSection({ title, count, table }: SectionProps) {
  return (
    <section className="plan-preview-panel__section">
      <div className="plan-preview-panel__section-title">
        <Typography.Text strong>{title}</Typography.Text>
        <Tag>{count}</Tag>
      </div>
      {table}
    </section>
  )
}

function SegmentTable({ preview, isRewind }: { preview: PlanPreviewVO; isRewind: boolean }) {
  return (
    <Table
      size="small"
      rowKey={segmentRowKey}
      pagination={false}
      columns={segmentColumns}
      dataSource={preview.segments ?? []}
      locale={{ emptyText: isRewind ? '后端未返回排布数据，请刷新预览或检查复卷配置' : '锯纸无分段排布' }}
      scroll={{ x: 620 }}
    />
  )
}

function FinishTable({ preview, isRewind }: { preview: PlanPreviewVO; isRewind: boolean }) {
  return (
    <Table
      size="small"
      rowKey={finishRowKey}
      pagination={false}
      columns={isRewind ? finishColumns : sawFinishColumns}
      dataSource={preview.finishes ?? []}
      locale={{ emptyText: preview.finishCount ? '后端返回了统计，但没有返回成品数据，请刷新预览' : '暂无成品数据' }}
      scroll={{ x: 620 }}
    />
  )
}

function sawPolicyOutcome(preview: PlanPreviewVO) {
  const weight = formatKgWithMaxDecimals(sawDifferenceWeight(preview), 2)
  if (preview.widthDifferencePolicy === 'LOSS') {
    return { color: 'orange', label: '计入损耗', detail: `${weight} 不生成库存` }
  }
  if (preview.widthDifferencePolicy === 'ALLOCATE') {
    return { color: 'blue', label: '分摊入成品', detail: `${weight} 已包含在成品重量内` }
  }
  return { color: 'cyan', label: '留作余料', detail: `${weight}，回录为 1 件独立余料` }
}

function sawDifferenceWeight(preview: PlanPreviewVO) {
  const fallback = preview.widthDifferencePolicy === 'LOSS'
    ? preview.calculatedLossWeight ?? preview.totalTrimWeight : preview.totalTrimWeight
  return preview.widthDifferenceWeight ?? fallback
}

interface SectionProps {
  title: string
  count: number
  table: ReactNode
}

function textCell(value?: string | number) {
  return <TooltipText value={value} />
}
