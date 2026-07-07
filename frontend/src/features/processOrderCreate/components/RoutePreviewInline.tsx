import { Descriptions, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type {
  ProcessRouteOutputVO,
  ProcessRoutePreviewVO,
  ProcessRouteStageLineVO,
} from '../../../types/processOrder'
import { formatKg, formatMoney } from '../../../utils/numberFormatters'
import { routeFinalOutputs } from './routePreviewInlineUtils'

interface Props {
  preview: ProcessRoutePreviewVO
}

export default function RoutePreviewInline({ preview }: Props) {
  const finals = routeFinalOutputs(preview)

  return (
    <Space direction="vertical" style={{ width: '100%' }}>
      <Descriptions size="small" column={3} bordered>
        <Descriptions.Item label="工序">{preview.stages?.length ?? 0} 道</Descriptions.Item>
        <Descriptions.Item label="最终成品">{finals.length} 件</Descriptions.Item>
        <Descriptions.Item label="加工费">{formatMoney(preview.totalAmount)}</Descriptions.Item>
      </Descriptions>
      <Typography.Text type="secondary">
        链式工艺按最终成品统计，中间产物只用于追溯和后续加工。
      </Typography.Text>
      <Table
        size="small"
        rowKey={stageRowKey}
        pagination={false}
        columns={stageColumns}
        dataSource={preview.stages ?? []}
      />
      <Table
        size="small"
        rowKey={(record) => record.outputKey ?? ''}
        pagination={false}
        columns={outputColumns}
        dataSource={preview.outputs ?? []}
      />
    </Space>
  )
}

function stageRowKey(record: ProcessRouteStageLineVO) {
  return `${record.stageLevel ?? 0}-${record.stepName ?? 'stage'}-${record.inputOutputKeys?.join('-') ?? 'root'}`
}

const stageColumns: ColumnsType<ProcessRouteStageLineVO> = [
  { title: '阶段', dataIndex: 'stageLevel', width: 80, render: (value) => `第 ${value ?? '-'} 道` },
  { title: '工艺', dataIndex: 'stepName', width: 100 },
  { title: '来源', dataIndex: 'inputOutputKeys', render: sourceText },
  { title: '费用', dataIndex: 'stepAmount', width: 110, render: formatMoney },
]

const outputColumns: ColumnsType<ProcessRouteOutputVO> = [
  { title: '产物', dataIndex: 'outputKey', width: 100 },
  { title: '状态', width: 110, render: (_, row) => outputStatus(row) },
  { title: '规格', render: (_, row) => `${row.gramWeight ?? '-'}g / ${row.finishWidth ?? '-'}mm` },
  { title: '预估重量', dataIndex: 'estimateWeight', width: 120, render: formatKg },
]

function sourceText(value?: string[]) {
  return Array.isArray(value) && value.length ? value.join('、') : '母卷'
}

function outputStatus(row: ProcessRouteOutputVO) {
  if (row.isRemain === 1) return <Tag color="orange">修边</Tag>
  return row.consumedByNextStage ? <Tag color="orange">进入下道</Tag> : <Tag color="green">最终成品</Tag>
}
