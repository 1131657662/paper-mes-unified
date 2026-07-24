import { Descriptions, Table, Tag, Tooltip, Typography } from 'antd'
import dayjs from 'dayjs'
import type { ColumnsType } from 'antd/es/table'
import type { ReportMetricVersionAuditVO } from '../../../types/report'

export default function ReportMetricVersionTable({ items }: { items: ReportMetricVersionAuditVO[] }) {
  return <Table<ReportMetricVersionAuditVO> className="report-metric-version-table"
    columns={columns} dataSource={items} pagination={{ pageSize: 10, showSizeChanger: false }} rowKey="metricVersionUuid"
    scroll={{ x: 730 }} size="small" expandable={{ expandedRowRender: DefinitionDetail }} />
}

const columns: ColumnsType<ReportMetricVersionAuditVO> = [
  {
    title: '指标', dataIndex: 'metricName', width: 180,
    render: (_, item) => <div className="report-metric-version-name">
      <strong>{item.metricName}</strong>
      <span>{item.metricCode}</span>
    </div>,
  },
  {
    title: '类型 / 单位', width: 116,
    render: (_, item) => <>{valueTypeLabel(item.valueType)}<br />
      <Typography.Text type="secondary">{item.unitCode} · {item.displayScale} 位</Typography.Text></>,
  },
  {
    title: '版本', dataIndex: 'versionNo', width: 92, align: 'center',
    render: (value: number, item) => <><strong>V{value}</strong><br />
      <Tag color={item.versionStatus === 2 ? 'success' : 'warning'}>
        {item.versionStatus === 2 ? '已锁定' : '草稿'}
      </Tag></>,
  },
  {
    title: '实现键', dataIndex: 'implementationKey', width: 190, ellipsis: { showTitle: false },
    render: (value: string) => <Tooltip title={value}>
      <Typography.Text code ellipsis>{value}</Typography.Text>
    </Tooltip>,
  },
  {
    title: '锁定时间', dataIndex: 'lockedAt', width: 132,
    render: (value?: string) => value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-',
  },
]

function DefinitionDetail(item: ReportMetricVersionAuditVO) {
  return <Descriptions className="report-metric-version-detail" column={1} size="small">
    <Descriptions.Item label="业务定义">{item.description || '-'}</Descriptions.Item>
    <Descriptions.Item label="版本 UUID">
      <Typography.Text copyable>{item.metricVersionUuid}</Typography.Text>
    </Descriptions.Item>
    <Descriptions.Item label="定义校验">
      <Typography.Text copyable>{item.definitionChecksum}</Typography.Text>
    </Descriptions.Item>
    <Descriptions.Item label="定义 JSON">
      <pre>{item.definitionJson}</pre>
    </Descriptions.Item>
    <Descriptions.Item label="锁定人">{item.lockedBy || '-'}</Descriptions.Item>
  </Descriptions>
}

function valueTypeLabel(value: ReportMetricVersionAuditVO['valueType']) {
  if (value === 'MONEY') return '金额'
  if (value === 'PERCENT') return '百分比'
  if (value === 'INTEGER') return '整数'
  return '小数'
}
