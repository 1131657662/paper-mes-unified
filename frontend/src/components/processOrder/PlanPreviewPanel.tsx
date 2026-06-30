import { Descriptions, Empty, Spin, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import TooltipText from '../biz/TooltipText'
import type { FinishPreviewVO, RewindFinishItemPreview, RewindSegmentDTO, RewindSegmentPreview } from '../../types/processOrder'

interface Props {
  segments: RewindSegmentDTO[]
  originalWidth: number | undefined
  preview: FinishPreviewVO | null
  spareCount: number
  loading: boolean
}

const segColumns: ColumnsType<RewindSegmentPreview> = [
  { title: '分段', dataIndex: 'segmentSort', width: 50, render: (v: number) => `#${v}` },
  {
    title: '分摊',
    dataIndex: 'segmentRatio',
    width: 70,
    render: (v: number) => `${v}%`,
  },
  { title: '目标直径', dataIndex: 'targetDiameter', width: 85, render: (v: number) => `${v}mm` },
  { title: '重复', dataIndex: 'repeatCount', width: 55 },
  { title: '排布宽度', dataIndex: 'layoutWidth', width: 85, render: (v: number) => `${v}mm` },
  { title: '修边', dataIndex: 'trimWidth', width: 70, render: (v: number) => (v ? `${v}mm` : '-') },
  { title: '汇总', dataIndex: 'summary', render: textCell },
]

const finishColumns: ColumnsType<RewindFinishItemPreview> = [
  { title: '分段', dataIndex: 'segmentSort', width: 50, render: (v: number) => `#${v}` },
  { title: '成品门幅', dataIndex: 'finishWidth', width: 85, render: (v: number) => `${v}mm` },
  { title: '成品直径', dataIndex: 'finishDiameter', width: 85, render: (v: number) => `${v}mm` },
  { title: '纸芯直径', dataIndex: 'finishCoreDiameter', width: 85, render: (v: number) => `${v}mm` },
  { title: '分摊比例', dataIndex: 'segmentRatio', width: 80, render: (v: number) => `${v}%` },
  { title: '预估重量', dataIndex: 'estimateWeight', width: 85, render: (v: number) => `${v?.toFixed(2)}kg` },
  { title: '修边宽度', dataIndex: 'trimWidth', width: 80, render: (v: number) => (v ? `${v}mm` : '-') },
  { title: '修边重量', dataIndex: 'trimWeight', width: 80, render: (v: number) => (v ? `${v.toFixed(2)}kg` : '-') },
  { title: '来源', dataIndex: 'sourceSummary', render: textCell },
]

export default function PlanPreviewPanel({ preview, loading }: Props) {
  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', flexDirection: 'column', gap: 10, height: 400 }}>
        <Spin />
        <Typography.Text type="secondary">正在生成预览方案...</Typography.Text>
      </div>
    )
  }

  if (!preview) {
    return <Empty description="点击左侧「预览方案」生成排布预览" style={{ marginTop: 120 }} />
  }

  const { segments, finishes, finishCount, trimCount, totalEstimateWeight, totalTrimWeight } = preview

  return (
    <div>
      <Descriptions size="small" column={2} style={{ marginBottom: 12 }}>
        <Descriptions.Item label="成品卷数">{finishCount ?? 0}</Descriptions.Item>
        <Descriptions.Item label="修边条数">{trimCount ?? 0}</Descriptions.Item>
        <Descriptions.Item label="预估总重">{totalEstimateWeight?.toFixed(2) ?? '-'} kg</Descriptions.Item>
        <Descriptions.Item label="修边总重">{totalTrimWeight ? `${totalTrimWeight.toFixed(2)} kg` : '-'}</Descriptions.Item>
      </Descriptions>

      <Typography.Text strong style={{ fontSize: 13 }}>分段排布</Typography.Text>
      <Table
        size="small"
        rowKey="segmentSort"
        columns={segColumns}
        dataSource={segments ?? []}
        pagination={false}
        style={{ marginBottom: 12 }}
        scroll={{ x: 500 }}
      />

      <Typography.Text strong style={{ fontSize: 13 }}>
        成品明细
        <Tag style={{ marginLeft: 8 }}>{finishes?.length ?? 0} 卷</Tag>
      </Typography.Text>
      <Table
        size="small"
        rowKey={(_, i) => String(i)}
        columns={finishColumns}
        dataSource={finishes ?? []}
        pagination={false}
        scroll={{ x: 750 }}
      />
    </div>
  )
}

function textCell(value?: string | number) {
  return <TooltipText value={value} />
}
