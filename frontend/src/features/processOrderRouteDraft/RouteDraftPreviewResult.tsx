import { Alert, Descriptions, Space, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type {
  ProcessRouteOutputVO,
  ProcessRoutePreviewVO,
  ProcessRouteStageLineVO,
} from '../../types/processOrder'
import {
  formatMoney,
  formatMm,
  formatOptionalKg,
  formatOptionalMoney,
  formatOptionalTon,
  formatOptionalTonFromKg,
} from '../../utils/numberFormatters'

interface Props {
  preview: ProcessRoutePreviewVO
}

export default function RouteDraftPreviewResult({ preview }: Props) {
  const finalCount = (preview.outputs ?? []).filter(isDeliverableOutput).length
  const finalWeight = (preview.outputs ?? [])
    .filter(isDeliverableOutput)
    .reduce((sum, item) => sum + Number(item.estimateWeight ?? 0), 0)

  return (
    <Space direction="vertical" size={10} className="route-draft-preview">
      <Alert
        type="success"
        showIcon
        message="预览校验通过"
        description="以下结果来自后端校验，会用于保存草稿、提交加工单后的工序、阶段产物、最终成品和加工费口径。"
      />
      <Descriptions size="small" column={4} bordered>
        <Descriptions.Item label="工序数">{preview.stages?.length ?? 0} 道</Descriptions.Item>
        <Descriptions.Item label="最终成品">{finalCount} 件</Descriptions.Item>
        <Descriptions.Item label="最终重量">{formatOptionalTonFromKg(finalWeight)}</Descriptions.Item>
        <Descriptions.Item label="加工费">{formatMoney(preview.totalAmount)}</Descriptions.Item>
      </Descriptions>
      <Table
        size="small"
        rowKey={stageRowKey}
        pagination={false}
        columns={stageColumns}
        dataSource={preview.stages ?? []}
        scroll={{ x: 760 }}
      />
      <Table
        size="small"
        rowKey={(record) => record.outputKey ?? ''}
        pagination={false}
        columns={outputColumns}
        dataSource={preview.outputs ?? []}
        scroll={{ x: 860 }}
      />
    </Space>
  )
}

const stageColumns: ColumnsType<ProcessRouteStageLineVO> = [
  { title: '阶段', dataIndex: 'stageLevel', width: 70, render: (value) => `第${value ?? '-'}段` },
  { title: '工艺', dataIndex: 'stepName', width: 90 },
  { title: '来源产物', dataIndex: 'inputOutputKeys', width: 130, render: sourceText },
  { title: '刀数', dataIndex: 'knifeCount', width: 76, render: (value) => value ?? '-' },
  { title: '吨位', dataIndex: 'processWeight', width: 100, render: formatOptionalTon },
  { title: '单价', dataIndex: 'unitPrice', width: 96, render: formatOptionalMoney },
  { title: '费用', dataIndex: 'stepAmount', width: 96, render: formatOptionalMoney },
]

const outputColumns: ColumnsType<ProcessRouteOutputVO> = [
  { title: '产出', dataIndex: 'outputKey', width: 90 },
  { title: '阶段', dataIndex: 'stageLevel', width: 70, render: (value) => `第${value ?? '-'}段` },
  { title: '状态', width: 100, render: (_, row) => outputStatus(row) },
  { title: '门幅', dataIndex: 'finishWidth', width: 90, render: (value) => value ? formatMm(value) : '-' },
  { title: '预估重', dataIndex: 'estimateWeight', width: 110, render: formatOptionalKg },
  { title: '备注', dataIndex: 'remark', width: 180 },
]

function sourceText(value?: string[]) {
  return Array.isArray(value) && value.length ? value.join('、') : '原卷'
}

function outputStatus(row: ProcessRouteOutputVO) {
  if (row.isRemain === 1) return <Tag color="orange">修边</Tag>
  return row.consumedByNextStage ? <Tag color="orange">进入下道</Tag> : <Tag color="green">最终成品</Tag>
}

function isDeliverableOutput(item: ProcessRouteOutputVO) {
  return item.isRemain !== 1 && !item.consumedByNextStage
}

function stageRowKey(record: ProcessRouteStageLineVO) {
  return `${record.stageLevel ?? 0}-${record.stepName ?? 'stage'}`
}
