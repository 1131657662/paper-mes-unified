import { SearchOutlined } from '@ant-design/icons'
import { Button, Empty, Table, Tooltip } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { ReportDimension, ReportDimensionVO, ReportMetricItemVO } from '../../types/report'
import { metricField } from './reportExplorerModel'

interface Props {
  dimension: ReportDimension
  metrics: ReportMetricItemVO[]
  rows: ReportDimensionVO[]
  onDrill: (key: string) => void
}

export default function ReportExplorerTable(props: Props) {
  if (!props.rows.length) return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前筛选暂无数据" />
  return <Table className="report-explorer-table" rowKey={(row) => `${props.dimension}-${row.dimensionKey}`}
    columns={columns(props)} dataSource={props.rows} pagination={false}
    scroll={{ x: 'max-content', y: 520 }} />
}

function columns(props: Props): ColumnsType<ReportDimensionVO> {
  return [
    { title: '分组', dataIndex: 'dimensionName', fixed: 'left', width: 220,
      render: (value: string, row) => <DimensionCell disabled={props.dimension === 'month'}
        label={value} onClick={() => props.onDrill(row.dimensionKey)} /> },
    ...props.metrics.map((metric) => ({ title: metric.metricName, width: 142, align: 'right' as const,
      render: (_: unknown, row: ReportDimensionVO) => formatValue(row, metric) })),
  ]
}

function DimensionCell(props: { disabled: boolean; label: string; onClick: () => void }) {
  if (props.disabled) return props.label
  return <Tooltip title="按该项继续筛选"><Button type="link" className="report-explorer-drill"
    icon={<SearchOutlined />} onClick={props.onClick}>{props.label}</Button></Tooltip>
}

function formatValue(row: ReportDimensionVO, metric: ReportMetricItemVO) {
  const field = metricField(metric.metricCode)
  const value = field ? row[field] : undefined
  if (typeof value !== 'number') return '-'
  return value.toLocaleString('zh-CN', { maximumFractionDigits: metric.displayScale ?? 2 })
}
