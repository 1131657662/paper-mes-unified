import { PlusOutlined } from '@ant-design/icons'
import { Alert, Button, Skeleton } from 'antd'
import dayjs from 'dayjs'
import { useState } from 'react'
import { useReportMetricReleases } from '../../features/report/hooks/useReportMetricReleases'
import ReportSubscriptionList from '../../features/reportSubscription/components/ReportSubscriptionList'
import ReportSubscriptionModal from '../../features/reportSubscription/components/ReportSubscriptionModal'
import ReportSubscriptionRunDrawer from '../../features/reportSubscription/components/ReportSubscriptionRunDrawer'
import { useDeleteReportSubscription } from '../../features/reportSubscription/hooks/useDeleteReportSubscription'
import { useReportSubscriptionCandidates } from '../../features/reportSubscription/hooks/useReportSubscriptionCandidates'
import { useReportSubscriptions } from '../../features/reportSubscription/hooks/useReportSubscriptions'
import type { ReportSubscription } from '../../features/reportSubscription/types'
import type { ReportQuery } from '../../types/report'

export default function ReportSubscriptionManagement() {
  const [editing, setEditing] = useState<ReportSubscription | null | undefined>(undefined)
  const [history, setHistory] = useState<ReportSubscription>()
  const subscriptions = useReportSubscriptions(true)
  const candidates = useReportSubscriptionCandidates(true)
  const releases = useReportMetricReleases(true)
  const deleteMutation = useDeleteReportSubscription()
  const retry = () => { void subscriptions.refetch(); void candidates.refetch(); void releases.refetch() }
  return <div className="report-management__content">
    <div className="report-management__toolbar">
      <div><strong>定时报表</strong><span>每个接收人会在自己的下载任务中心收到独立文件。</span></div>
      <Button type="primary" icon={<PlusOutlined />} onClick={() => setEditing(null)}>新建订阅</Button>
    </div>
    {(subscriptions.isError || candidates.isError || releases.isError) && <Alert showIcon type="error"
      message="订阅资料加载失败" action={<Button size="small" onClick={retry}>重试</Button>} />}
    {subscriptions.isLoading ? <Skeleton active paragraph={{ rows: 6 }} /> :
      <ReportSubscriptionList deleting={deleteMutation.isPending} items={subscriptions.data ?? []}
        onDelete={(item) => deleteMutation.mutate({ uuid: item.uuid, version: item.version })}
        onEdit={setEditing} onHistory={setHistory} />}
    {editing !== undefined && <ReportSubscriptionModal key={editing?.uuid ?? 'create'} open
      candidates={candidates.data ?? []} currentQuery={defaultQuery()} initial={editing}
      reportPath={editing?.reportPath ?? '/reports/overview'}
      releases={releases.data ?? []} onClose={() => setEditing(undefined)} onSaved={() => setEditing(undefined)} />}
    {history && <ReportSubscriptionRunDrawer open subscription={history} onClose={() => setHistory(undefined)} />}
  </div>
}

function defaultQuery(): ReportQuery {
  return { dateFrom: dayjs().startOf('year').format('YYYY-MM-DD'), dateTo: dayjs().format('YYYY-MM-DD') }
}
