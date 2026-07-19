import { lazy, Suspense } from 'react'
import type { DashboardTrend as DashboardTrendItem } from '../../../types/dashboard'
import { useNearViewport } from '../../../hooks/useNearViewport'
import { formatMoney, formatNumber } from '../../report/utils/reportFormatters'
import DashboardPanelHead from './DashboardPanelHead'
import { buildTrendModel, type DashboardTrendModel } from './dashboardTrendModel'

const DashboardTrendChart = lazy(() => import('./DashboardTrendChart'))

interface Props {
  monthly: DashboardTrendItem[]
}

export default function DashboardTrend({ monthly }: Props) {
  const model = buildTrendModel(monthly)
  const { isNearViewport, targetRef } = useNearViewport<HTMLDivElement>()
  return (
    <section className="dashboard-panel dashboard-trend">
      <DashboardPanelHead title="分析概览" subtitle="近12个月加工应收变化，本月数据截至今日。" />
      <TrendSummary model={model} />
      <div ref={targetRef} className="dashboard-trend__line-chart">
        {isNearViewport ? (
          <Suspense fallback={<TrendChartLoading />}>
            <DashboardTrendChart model={model} />
          </Suspense>
        ) : <TrendChartLoading />}
        {!model.hasReceivable && <TrendEmptyState />}
      </div>
    </section>
  )
}

function TrendSummary({ model }: { model: DashboardTrendModel }) {
  return (
    <div className="dashboard-trend__summary">
      <div><span>近12月加工应收</span><strong>{formatMoney(model.totalAmount)}</strong></div>
      <div><span>完成加工单</span><strong>{formatNumber(model.totalOrders)} 单</strong></div>
      <div><span>近12月月均</span><strong>{formatMoney(model.averageAmount)}</strong></div>
    </div>
  )
}

function TrendChartLoading() {
  return <div className="dashboard-trend__chart-loading" aria-label="趋势图加载中" />
}

function TrendEmptyState() {
  return <div className="dashboard-trend__empty"><strong>暂无近12个月加工应收</strong>
    <span>完成加工后，这里会自动形成月度应收趋势。</span></div>
}
