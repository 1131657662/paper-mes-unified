import { Button } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useNavigate } from 'react-router-dom'
import DocumentDetailTable from '../../../components/biz/DocumentDetailTable'
import type { ReportDetailVO } from '../../../types/report'
import { formatPercent, formatTonFromKg } from '../utils/reportFormatters'

export default function ReportLossLeaders({ rows }: { rows: ReportDetailVO[] }) {
  const navigate = useNavigate()
  const columns: ColumnsType<ReportDetailVO> = [
    { title: '加工单号', dataIndex: 'orderNo', width: 150, fixed: 'left' },
    { title: '客户', dataIndex: 'customerName', width: 180, ellipsis: true },
    { title: '品名 / 规格', dataIndex: 'paperSummary', width: 240, ellipsis: true },
    { title: '投入', dataIndex: 'originalWeight', width: 110, align: 'right', render: formatTonFromKg },
    { title: '损耗', dataIndex: 'lossWeight', width: 110, align: 'right', render: formatTonFromKg },
    { title: '损耗率', dataIndex: 'lossRatio', width: 100, align: 'right', render: formatPercent },
    { title: '操作', key: 'action', width: 90, fixed: 'right', align: 'center',
      render: (_, row) => <Button type="link" onClick={() => navigate(`/process-orders/${row.orderUuid}`)}>追溯</Button> },
  ]
  return (
    <section className="report-topic-panel report-loss-leaders">
      <header><div><h3>高损耗加工单</h3><p>由数据库按损耗率、损耗重量排序，点击进入加工单核对来源。</p></div></header>
      <DocumentDetailTable<ReportDetailVO>
        columns={columns}
        dataSource={rows}
        pagination={{ pageSize: 5, showSizeChanger: false }}
        rowKey="orderUuid"
        scroll={{ x: 980, y: 420 }}
        storageKey="report-quality-loss-leaders"
      />
    </section>
  )
}
