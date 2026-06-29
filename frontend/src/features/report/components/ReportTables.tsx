import { Table, Tabs } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type {
  CustomerReportVO,
  LossReportVO,
  MachineReportVO,
  MonthlyReportVO,
} from '../../../types/report'
import { formatKg, formatMoney, formatNumber, formatPercent, formatTon } from '../utils/reportFormatters'

interface Props {
  customers: CustomerReportVO[]
  losses: LossReportVO[]
  machines: MachineReportVO[]
  monthly: MonthlyReportVO[]
}

export default function ReportTables({ customers, losses, machines, monthly }: Props) {
  return (
    <section className="report-panel report-panel--tables">
      <Tabs
        items={[
          { key: 'monthly', label: '月度汇总', children: <MonthlyTable data={monthly} /> },
          { key: 'customers', label: '客户统计', children: <CustomerTable data={customers} /> },
          { key: 'losses', label: '损耗分析', children: <LossTable data={losses} /> },
          { key: 'machines', label: '机台产出', children: <MachineTable data={machines} /> },
        ]}
      />
    </section>
  )
}

function MonthlyTable({ data }: { data: MonthlyReportVO[] }) {
  return <Table className="mes-table-card" rowKey="month" size="small" columns={monthlyColumns} dataSource={data} pagination={false} scroll={{ x: 760 }} />
}

function CustomerTable({ data }: { data: CustomerReportVO[] }) {
  return <Table className="mes-table-card" rowKey="customerUuid" size="small" columns={customerColumns} dataSource={data} pagination={false} scroll={{ x: 680 }} />
}

function LossTable({ data }: { data: LossReportVO[] }) {
  return <Table className="mes-table-card" rowKey="month" size="small" columns={lossColumns} dataSource={data} pagination={false} scroll={{ x: 680 }} />
}

function MachineTable({ data }: { data: MachineReportVO[] }) {
  return <Table className="mes-table-card" rowKey="machineUuid" size="small" columns={machineColumns} dataSource={data} pagination={false} scroll={{ x: 680 }} />
}

const monthlyColumns: ColumnsType<MonthlyReportVO> = [
  { title: '月份', dataIndex: 'month', width: 110 },
  { title: '加工单', dataIndex: 'orderCount', width: 100, render: (value) => `${formatNumber(value)} 单` },
  { title: '原纸吨位', dataIndex: 'totalTon', align: 'right', width: 130, render: (value) => formatTon(value) },
  { title: '成品重量', dataIndex: 'totalFinishWeight', align: 'right', width: 140, render: (value) => formatKg(value) },
  { title: '刀数', dataIndex: 'totalKnife', align: 'right', width: 100, render: (value) => formatNumber(value) },
  { title: '金额', dataIndex: 'totalAmount', align: 'right', width: 130, render: (value) => formatMoney(value) },
]

const customerColumns: ColumnsType<CustomerReportVO> = [
  { title: '客户', dataIndex: 'customerName', width: 180, ellipsis: true },
  { title: '加工单', dataIndex: 'orderCount', width: 100, render: (value) => `${formatNumber(value)} 单` },
  { title: '原纸吨位', dataIndex: 'totalTon', align: 'right', width: 130, render: (value) => formatTon(value) },
  { title: '刀数', dataIndex: 'totalKnife', align: 'right', width: 100, render: (value) => formatNumber(value) },
  { title: '金额', dataIndex: 'totalAmount', align: 'right', width: 130, render: (value) => formatMoney(value) },
]

const lossColumns: ColumnsType<LossReportVO> = [
  { title: '月份', dataIndex: 'month', width: 110 },
  { title: '原卷', dataIndex: 'rollCount', width: 100, render: (value) => `${formatNumber(value)} 卷` },
  { title: '原纸重量', dataIndex: 'totalOriginalWeight', align: 'right', width: 140, render: (value) => formatKg(value) },
  { title: '损耗重量', dataIndex: 'totalLossWeight', align: 'right', width: 140, render: (value) => formatKg(value) },
  { title: '平均损耗率', dataIndex: 'avgLossRatio', align: 'right', width: 130, render: (value) => formatPercent(value) },
]

const machineColumns: ColumnsType<MachineReportVO> = [
  { title: '机台', dataIndex: 'machineName', width: 180, ellipsis: true },
  { title: '加工卷数', dataIndex: 'rollCount', width: 110, render: (value) => `${formatNumber(value)} 卷` },
  { title: '产出重量', dataIndex: 'totalOutputWeight', align: 'right', width: 140, render: (value) => formatKg(value) },
  { title: '刀数', dataIndex: 'totalKnife', align: 'right', width: 100, render: (value) => formatNumber(value) },
  { title: '损耗重量', dataIndex: 'totalLossWeight', align: 'right', width: 140, render: (value) => formatKg(value) },
]
