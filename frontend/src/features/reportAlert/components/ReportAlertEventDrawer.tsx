import { SearchOutlined } from '@ant-design/icons'
import { Alert, Button, Drawer, Empty, Input, Pagination, Segmented, Skeleton, Space } from 'antd'
import { useState } from 'react'
import { PERMISSIONS } from '../../../constants/permissions'
import { useHasPermission } from '../../../stores/authStore'
import { useAcknowledgeReportAlertEvent } from '../hooks/useAcknowledgeReportAlertEvent'
import { useIgnoreReportAlertEvent } from '../hooks/useIgnoreReportAlertEvent'
import { useReportAlertEvents } from '../hooks/useReportAlertEvents'
import type { ReportAlertEventStatus } from '../types'
import ReportAlertEventCard from './ReportAlertEventCard'
import './ReportAlertEvent.css'

interface Props { focusedUuid?: string; onClose: () => void; open: boolean }

export default function ReportAlertEventDrawer({ focusedUuid, onClose, open }: Props) {
  const [status, setStatus] = useState<ReportAlertEventStatus>(1)
  const [page, setPage] = useState(1)
  const [keyword, setKeyword] = useState('')
  const [draftKeyword, setDraftKeyword] = useState('')
  const query = { page, size: 8, status, keyword: keyword || undefined, focusUuid: focusedUuid }
  const { data, isLoading, isError, refetch } = useReportAlertEvents(query, open)
  const { mutate: acknowledge, isPending: isAcknowledging } = useAcknowledgeReportAlertEvent()
  const { mutate: ignore, isPending: isIgnoring } = useIgnoreReportAlertEvent()
  const canManage = useHasPermission(PERMISSIONS.systemConfig)
  const busy = isAcknowledging || isIgnoring
  const tabs = [
    { label: `活动 ${data?.activeCount ?? 0}`, value: 1 },
    { label: `已恢复 ${data?.resolvedCount ?? 0}`, value: 2 },
    { label: `已忽略 ${data?.ignoredCount ?? 0}`, value: 3 },
  ] satisfies Array<{ label: string; value: ReportAlertEventStatus }>
  const search = () => { setKeyword(draftKeyword.trim()); setPage(1) }
  const changeStatus = (value: ReportAlertEventStatus) => { setStatus(value); setPage(1) }
  return <Drawer className="report-alert-event-drawer" title="预警事件工作台" width="min(720px, calc(100vw - 24px))"
    open={open} onClose={onClose} destroyOnHidden>
    <div className="report-alert-event-toolbar">
      <Segmented block aria-label="预警事件状态" options={tabs} value={status}
        onChange={(value) => changeStatus(value as ReportAlertEventStatus)} />
      <Space.Compact className="report-alert-event-search">
        <Input allowClear value={draftKeyword} placeholder="搜索规则、客户或纸张" onChange={(event) => setDraftKeyword(event.target.value)}
          onPressEnter={search} />
        <Button icon={<SearchOutlined />} onClick={search} aria-label="搜索预警" />
      </Space.Compact>
    </div>
    {focusedUuid && <Alert className="report-alert-event-focus" type="info" showIcon
      message="已定位通知中的预警事件" action={<Button size="small" onClick={() => onClose()}>清除定位</Button>} />}
    {isError && <Alert type="error" showIcon message="预警事件加载失败" action={<Button size="small" onClick={() => void refetch()}>重试</Button>} />}
    {isLoading ? <Skeleton active paragraph={{ rows: 8 }} /> : <>
      <div className="report-alert-event-list">
        {(data?.items ?? []).map((event) => <ReportAlertEventCard key={event.uuid} event={event} canManage={canManage}
          busy={busy} onAcknowledge={acknowledge}
          onIgnore={(uuid, reason) => ignore({ uuid, reason })} />)}
        {!data?.items.length && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前视图暂无预警事件" />}
      </div>
      {!!data?.total && <Pagination className="report-alert-event-pagination" current={data.page} pageSize={data.size}
        total={data.total} showSizeChanger={false} onChange={(nextPage) => setPage(nextPage)} />}
    </>}
  </Drawer>
}
