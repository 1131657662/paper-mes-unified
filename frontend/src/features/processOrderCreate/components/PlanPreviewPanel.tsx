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
import { formatKg } from '../../../utils/numberFormatters'
import './PlanPreviewPanel.css'

interface Props {
  preview?: PlanPreviewVO
  loading?: boolean
  onPreview?: () => void
}

const segmentColumns: ColumnsType<RewindSegmentPreview> = [
  { title: '段', dataIndex: 'segmentSort', width: 52, render: (value) => `#${value ?? '-'}` },
  { title: '比例', dataIndex: 'segmentRatio', width: 72, render: (value) => `${value ?? '-'}%` },
  { title: '目标直径', dataIndex: 'targetDiameter', width: 88, render: (value) => value ? `${value}mm` : '-' },
  { title: '重复', dataIndex: 'repeatCount', width: 58 },
  { title: '排布', dataIndex: 'layoutWidth', width: 84, render: (value) => value ? `${value}mm` : '-' },
  { title: '修边', dataIndex: 'trimWidth', width: 72, render: (value) => value ? `${value}mm` : '-' },
  { title: '摘要', dataIndex: 'summary', width: 180, render: textCell },
]

const finishColumns: ColumnsType<RewindFinishItemPreview> = [
  { title: '段', dataIndex: 'segmentSort', width: 52, render: (value) => value ? `#${value}` : '-' },
  { title: '门幅', dataIndex: 'finishWidth', width: 72, render: (value) => `${value ?? 0}mm` },
  { title: '直径', dataIndex: 'finishDiameter', width: 76, render: (value) => value ? `${value}mm` : '-' },
  { title: '纸芯', dataIndex: 'finishCoreDiameter', width: 72, render: (value) => value ? `${value}mm` : '-' },
  { title: '预估重', dataIndex: 'estimateWeight', width: 92, render: (value) => formatKg(Number(value ?? 0)) },
  { title: '修边重', dataIndex: 'trimWeight', width: 92, render: (value) => formatKg(Number(value ?? 0)) },
  { title: '来源', dataIndex: 'sourceSummary', width: 160, render: textCell },
]

export default function PlanPreviewPanel({ preview, loading, onPreview }: Props) {
  return (
    <div className="plan-preview-panel">
      <PreviewToolbar preview={preview} loading={loading} onPreview={onPreview} />
      {!preview ? <EmptyPreview /> : <PreviewContent preview={preview} />}
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

function PreviewContent({ preview }: { preview: PlanPreviewVO }) {
  const isRewind = preview.mainStepType === 2
  return (
    <div className="plan-preview-panel__content">
      {preview.errors?.length ? <Alert type="error" showIcon message={preview.errors.join('；')} /> : null}
      <Typography.Paragraph className="plan-preview-panel__summary">
        {preview.summary || '暂无摘要'}
      </Typography.Paragraph>
      <PreviewStats preview={preview} />
      <PreviewTableSection
        title="排布数据"
        count={preview.segments?.length ?? 0}
        table={<SegmentTable preview={preview} isRewind={isRewind} />}
      />
      <PreviewTableSection
        title="成品数据"
        count={preview.finishes?.length ?? 0}
        table={<FinishTable preview={preview} />}
      />
    </div>
  )
}

function PreviewStats({ preview }: { preview: PlanPreviewVO }) {
  return (
    <Descriptions size="small" column={1} bordered className="plan-preview-panel__stats">
      <Descriptions.Item label="成品">{preview.finishCount ?? 0}</Descriptions.Item>
      <Descriptions.Item label="修边">{preview.trimCount ?? 0}</Descriptions.Item>
      <Descriptions.Item label="备用">{preview.spareCount ?? 0}</Descriptions.Item>
      <Descriptions.Item label="成品重">{formatKg(preview.totalEstimateWeight)}</Descriptions.Item>
      <Descriptions.Item label="修边重">{formatKg(preview.totalTrimWeight)}</Descriptions.Item>
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
      rowKey={(_, index) => `${preview.originalUuid}-segment-${index}`}
      pagination={false}
      columns={segmentColumns}
      dataSource={preview.segments ?? []}
      locale={{ emptyText: isRewind ? '后端未返回排布数据，请刷新预览或检查复卷配置' : '锯纸无分段排布' }}
      scroll={{ x: 620 }}
    />
  )
}

function FinishTable({ preview }: { preview: PlanPreviewVO }) {
  return (
    <Table
      size="small"
      rowKey={(_, index) => `${preview.originalUuid}-finish-${index}`}
      pagination={false}
      columns={finishColumns}
      dataSource={preview.finishes ?? []}
      locale={{ emptyText: preview.finishCount ? '后端返回了统计，但没有返回成品数据，请刷新预览' : '暂无成品数据' }}
      scroll={{ x: 620 }}
    />
  )
}

interface SectionProps {
  title: string
  count: number
  table: ReactNode
}

function textCell(value?: string | number) {
  return <TooltipText value={value} />
}
