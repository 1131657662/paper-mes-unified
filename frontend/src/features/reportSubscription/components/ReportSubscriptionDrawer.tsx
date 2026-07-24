import { PlusOutlined } from '@ant-design/icons'
import { Alert, Button, Drawer, Skeleton } from 'antd'
import { useState } from 'react'
import type { ReportQuery, ReportSourcePath } from '../../../types/report'
import { useReportMetricReleases } from '../../report/hooks/useReportMetricReleases'
import { useDeleteReportSubscription } from '../hooks/useDeleteReportSubscription'
import { useReportSubscriptionCandidates } from '../hooks/useReportSubscriptionCandidates'
import { useReportSubscriptions } from '../hooks/useReportSubscriptions'
import type { ReportSubscription } from '../types'
import ReportSubscriptionList from './ReportSubscriptionList'
import ReportSubscriptionModal from './ReportSubscriptionModal'
import ReportSubscriptionRunDrawer from './ReportSubscriptionRunDrawer'

interface Props {
  currentQuery: ReportQuery
  onClose: () => void
  open: boolean
  reportPath: ReportSourcePath
}

export default function ReportSubscriptionDrawer({ currentQuery, onClose, open, reportPath }: Props) {
  const [editing, setEditing] = useState<ReportSubscription | null | undefined>(undefined)
  const [history, setHistory] = useState<ReportSubscription | undefined>()
  const { data: subscriptions = [], isLoading: isLoadingSubscriptions,
    isError: isSubscriptionsError, refetch: refetchSubscriptions } = useReportSubscriptions(open)
  const { data: candidates = [], isError: isCandidatesError,
    refetch: refetchCandidates } = useReportSubscriptionCandidates(open)
  const { data: releases = [] } = useReportMetricReleases(open)
  const { mutate: deleteSubscription, isPending: isDeleting } = useDeleteReportSubscription()
  const retry = () => { void refetchSubscriptions(); void refetchCandidates() }
  return <Drawer className="report-subscription-drawer" width={820} open={open} onClose={onClose}
    title="报表订阅" extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => setEditing(null)}>新建订阅</Button>}>
    {(isSubscriptionsError || isCandidatesError) && <Alert className="report-subscription-error"
      type="error" showIcon message="订阅数据加载失败" action={<Button size="small" onClick={retry}>重试</Button>} />}
    {isLoadingSubscriptions ? <Skeleton active paragraph={{ rows: 5 }} /> :
      <ReportSubscriptionList deleting={isDeleting} items={subscriptions}
        onDelete={(item) => deleteSubscription({ uuid: item.uuid, version: item.version })}
        onEdit={setEditing} onHistory={setHistory} />}
    {editing !== undefined && <ReportSubscriptionModal key={editing?.uuid ?? 'create'} open
      candidates={candidates} currentQuery={currentQuery} initial={editing}
      reportPath={reportPath}
      releases={releases}
      onClose={() => setEditing(undefined)} onSaved={() => setEditing(undefined)} />}
    {history && <ReportSubscriptionRunDrawer open subscription={history}
      onClose={() => setHistory(undefined)} />}
  </Drawer>
}
