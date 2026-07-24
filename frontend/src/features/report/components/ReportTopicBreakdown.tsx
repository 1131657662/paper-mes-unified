import type { ColumnsType } from 'antd/es/table'
import DocumentDetailTable from '../../../components/biz/DocumentDetailTable'
import type { ReportDimensionVO } from '../../../types/report'
import { formatNumber, formatPercent, formatTonFromKg } from '../utils/reportFormatters'

interface Props {
  pageSize?: number
  rows: ReportDimensionVO[]
  storageKey: string
  title: string
}

export default function ReportTopicBreakdown({ pageSize, rows, storageKey, title }: Props) {
  return (
    <section className="report-topic-panel report-topic-breakdown">
      <header><div><h3>{title}</h3><p>按统一投入、产出和损耗口径横向比较。</p></div></header>
      <DocumentDetailTable<ReportDimensionVO>
        columns={columns}
        dataSource={rows}
        pagination={pageSize ? { pageSize, showSizeChanger: false } : false}
        rowKey="dimensionKey"
        scroll={{ x: 700, y: 360 }}
        storageKey={storageKey}
      />
    </section>
  )
}

const columns: ColumnsType<ReportDimensionVO> = [
  { title: '分类', dataIndex: 'dimensionName', width: 170, fixed: 'left', ellipsis: true },
  { title: '加工单', dataIndex: 'orderCount', width: 90, align: 'right',
    render: (value: number) => formatNumber(value) },
  { title: '投入', dataIndex: 'originalWeight', width: 120, align: 'right', render: formatTonFromKg },
  { title: '产出', dataIndex: 'finishWeight', width: 120, align: 'right', render: formatTonFromKg },
  { title: '损耗', dataIndex: 'lossWeight', width: 120, align: 'right', render: formatTonFromKg },
  { title: '损耗率', dataIndex: 'lossRatio', width: 100, align: 'right', render: formatPercent },
]
