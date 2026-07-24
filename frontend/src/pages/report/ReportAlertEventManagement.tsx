import { SearchOutlined } from '@ant-design/icons'
import { Alert, Button, Empty, Input, Pagination, Segmented, Skeleton, Space } from 'antd'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import ReportAlertEventCard from '../../features/reportAlert/components/ReportAlertEventCard'
import { useAcknowledgeReportAlertEvent } from '../../features/reportAlert/hooks/useAcknowledgeReportAlertEvent'
import { useIgnoreReportAlertEvent } from '../../features/reportAlert/hooks/useIgnoreReportAlertEvent'
import { useReportAlertEvents } from '../../features/reportAlert/hooks/useReportAlertEvents'
import type { ReportAlertEventStatus } from '../../features/reportAlert/types'
import { PERMISSIONS } from '../../constants/permissions'
import { useHasPermission } from '../../stores/authStore'
import '../../features/reportAlert/components/ReportAlertEvent.css'

export default function ReportAlertEventManagement({ focusedUuid }: { focusedUuid?: string }) {
  const [, setParams] = useSearchParams()
  const [status, setStatus] = useState<ReportAlertEventStatus>(1)
  const [page, setPage] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [draftKeyword, setDraftKeyword] = useState('')
  const events = useReportAlertEvents({ page, size: 8, status, keyword: keyword || undefined, focusUuid: focusedUuid }, true)
  const acknowledgeMutation = useAcknowledgeReportAlertEvent()
  const ignoreMutation = useIgnoreReportAlertEvent()
  const canManage = useHasPermission(PERMISSIONS.systemConfig)
  const busy = acknowledgeMutation.isPending || ignoreMutation.isPending
  const search = () => { setKeyword(draftKeyword.trim()); setPage(1) }
  const changeStatus = (value: ReportAlertEventStatus) => { setStatus(value); setPage(1) }
  const clearFocus = () => setParams((current) => {
    const next = new URLSearchParams(current)
    next.delete('eventId')
    next.delete('alertEvent')
    return next
  }, { replace: true })
  const tabs = [
    { label: `活动 ${events.data?.activeCount ?? 0}`, value: 1 },
    { label: `已恢复 ${events.data?.resolvedCount ?? 0}`, value: 2 },
    { label: `已忽略 ${events.data?.ignoredCount ?? 0}`, value: 3 },
  ] satisfies Array<{ label: string; value: ReportAlertEventStatus }>
  return <div className="report-management__content">
    <div className="report-alert-management__toolbar">
      <Segmented aria-label="预警事件状态" options={tabs} value={status}
        onChange={(value) => changeStatus(value as ReportAlertEventStatus)} />
      <Space.Compact className="report-alert-management__search">
        <Input allowClear aria-label="搜索预警事件" value={draftKeyword} placeholder="搜索规则、客户或纸张"
          onChange={(event) => setDraftKeyword(event.target.value)} onPressEnter={search} />
        <Button icon={<SearchOutlined />} onClick={search} aria-label="搜索预警" />
      </Space.Compact>
    </div>
    {focusedUuid && <Alert showIcon type="info" message="已定位通知中的预警事件"
      action={<Button size="small" onClick={clearFocus}>清除定位</Button>} />}
    {events.isError && <Alert showIcon type="error" message="预警事件加载失败"
      action={<Button size="small" onClick={() => void events.refetch()}>重试</Button>} />}
    {events.isLoading ? <Skeleton active paragraph={{ rows: 8 }} /> : <>
      <div className="report-alert-event-list">
        {(events.data?.items ?? []).map((event) => <ReportAlertEventCard key={event.uuid} event={event}
          canManage={canManage} busy={busy} onAcknowledge={acknowledgeMutation.mutate}
          onIgnore={(uuid, reason) => ignoreMutation.mutate({ uuid, reason })} />)}
        {!events.data?.items.length && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前视图暂无预警事件" />}
      </div>
      {!!events.data?.total && <Pagination className="report-alert-event-pagination" current={events.data.page}
        pageSize={events.data.size} total={events.data.total} showSizeChanger={false} onChange={setPage} />}
    </>}
  </div>
}
