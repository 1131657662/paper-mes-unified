import { useNavigate } from 'react-router-dom'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import DashboardActivityTimeline from '../../features/dashboard/components/DashboardActivityTimeline'
import DashboardHeader from '../../features/dashboard/components/DashboardHeader'
import DashboardMetricGrid from '../../features/dashboard/components/DashboardMetricGrid'
import DashboardOrderList from '../../features/dashboard/components/DashboardOrderList'
import DashboardRankList from '../../features/dashboard/components/DashboardRankList'
import DashboardSkeleton from '../../features/dashboard/components/DashboardSkeleton'
import DashboardTodoList from '../../features/dashboard/components/DashboardTodoList'
import DashboardTrend from '../../features/dashboard/components/DashboardTrend'
import DashboardWorkbenchPanel from '../../features/dashboard/components/DashboardWorkbenchPanel'
import DashboardWorkQueue from '../../features/dashboard/components/DashboardWorkQueue'
import { useDashboardOverview } from '../../features/dashboard/hooks/useDashboardOrders'
import { useAuthUser } from '../../stores/authStore'
import './DashboardPage.css'

export default function DashboardPage() {
  const navigate = useNavigate()
  const user = useAuthUser()
  const {
    data: overview,
    isFetching: isRefreshingOverview,
    isError: isOverviewError,
    isLoading: isLoadingOverview,
    refetch: refreshOverview,
  } = useDashboardOverview()
  const showInitialSkeleton = isLoadingOverview && !overview
  const goOrdersByStatus = (status: number) => navigate(`/process-orders?orderStatus=${status}`)

  return (
    <div className="dashboard-page">
      <DashboardHeader
        loading={isRefreshingOverview}
        metrics={overview?.metrics}
        onOpenReports={() => navigate('/reports')}
        onRefresh={() => refreshOverview()}
        statusQueue={overview?.statusQueue ?? []}
        todos={overview?.todos ?? []}
        userName={user?.realName ?? user?.username ?? '操作员'}
      />

      {isOverviewError && !overview ? (
        <QueryLoadErrorAlert
          description="经营指标和待办未成功加载，当前空白不代表没有业务数据。"
          message="仪表盘数据加载失败"
          onRetry={() => void refreshOverview()}
        />
      ) : showInitialSkeleton ? (
        <DashboardSkeleton />
      ) : (
        <div className="dashboard-page__content">
          <DashboardMetricGrid metrics={overview?.metrics} />
          <div className="dashboard-page__analysis-grid">
            <DashboardTrend monthly={overview?.yearlyTrend ?? overview?.monthlyTrend ?? []} />
            <DashboardWorkbenchPanel
              onNavigate={(path) => navigate(path)}
              statusQueue={overview?.statusQueue ?? []}
              todos={overview?.todos ?? []}
            />
          </div>
          <div className="dashboard-page__work-grid">
            <DashboardWorkQueue items={overview?.statusQueue ?? []} onOpenOrders={goOrdersByStatus} />
            <DashboardTodoList items={overview?.todos ?? []} onOpen={(path) => navigate(path || '/process-orders')} />
            <DashboardActivityTimeline orders={overview?.recentOrders ?? []} />
          </div>
          <div className="dashboard-page__bottom-grid">
            <DashboardOrderList
              loading={isLoadingOverview}
              onOpenOrder={(uuid) => navigate(`/process-orders/${uuid}`)}
              onOpenOrders={() => navigate('/process-orders')}
              orders={overview?.recentOrders ?? []}
            />
            <DashboardRankList
              emptyText="本月暂无客户加工统计"
              items={overview?.customerRank ?? []}
              yearlyItems={overview?.customerYearRank ?? []}
              yearlyEmptyText="本年暂无客户加工统计"
              mode="amount"
              subtitle="按加工应收排序，可切换本月和本年。"
              title="客户加工应收排行"
            />
            <DashboardRankList
              emptyText="本月暂无机台产出统计"
              items={overview?.machineRank ?? []}
              yearlyItems={overview?.machineYearRank ?? []}
              yearlyEmptyText="本年暂无机台产出统计"
              mode="weight"
              subtitle="按完成加工单产出重量排序，可切换本月和本年。"
              title="机台产出排行"
            />
          </div>
        </div>
      )}
    </div>
  )
}
