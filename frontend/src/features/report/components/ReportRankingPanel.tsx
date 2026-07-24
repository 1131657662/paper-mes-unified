import { Tabs } from 'antd'
import type { ReportDimensionVO } from '../../../types/report'
import { formatMoney, formatNumber, formatTonFromKg } from '../utils/reportFormatters'
import ReportBarList, { type ReportBarItem } from './ReportBarList'

interface Props {
  customers: ReportDimensionVO[]
  products: ReportDimensionVO[]
}

export default function ReportRankingPanel({ customers, products }: Props) {
  return (
    <section className="report-panel report-panel--rank">
      <div className="report-panel__head">
        <h3>结构排行</h3>
      <p>聚焦应收贡献与产品加工费结构，默认展示前 8 项。</p>
      </div>
      <Tabs
        className="report-ranking-tabs"
        defaultActiveKey="customer"
        items={[
          {
            key: 'customer', label: '客户应收',
            children: <ReportBarList emptyText="当前条件暂无客户汇总" items={customerItems(customers)} />,
          },
          {
            key: 'product', label: '产品加工费',
            children: <ReportBarList emptyText="当前条件暂无产品汇总" items={productItems(products)} />,
          },
        ]}
      />
    </section>
  )
}

function customerItems(rows: ReportDimensionVO[]): ReportBarItem[] {
  return rows.slice(0, 8).map((item) => ({
    key: item.dimensionKey,
    label: item.dimensionName,
    meta: `${formatNumber(item.orderCount)} 单 / ${formatTonFromKg(item.originalWeight)}`,
    value: item.totalAmount ?? 0,
    valueText: formatMoney(item.totalAmount),
  }))
}

function productItems(rows: ReportDimensionVO[]): ReportBarItem[] {
  return rows.slice(0, 8).map((item) => ({
    key: item.dimensionKey,
    label: item.dimensionName,
    meta: `${formatNumber(item.originalRollCount)} 卷 / ${formatTonFromKg(item.originalWeight)}`,
    value: item.processAmount ?? 0,
    valueText: formatMoney(item.processAmount),
  }))
}
