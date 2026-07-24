import { CloudDownloadOutlined } from '@ant-design/icons'
import { Badge, Button, Drawer, Tooltip } from 'antd'
import { useEffect, useState } from 'react'
import { PERMISSIONS } from '../constants/permissions'
import { useExportTaskEvents } from '../features/exportTask/hooks/useExportTaskEvents'
import { useExportTasks } from '../features/exportTask/hooks/useExportTasks'
import { useAuthUser, useHasPermission } from '../stores/authStore'
import DownloadTaskDrawerContent from './DownloadTaskDrawerContent'
import { OPEN_DOWNLOAD_TASK_CENTER_EVENT } from './downloadTaskCenterEvents'
import { useDownloadTaskHandlers } from './useDownloadTaskHandlers'
import './DownloadTaskCenter.css'

export default function DownloadTaskCenter() {
  const [open, setOpen] = useState(false)
  const user = useAuthUser()
  const canViewTasks = useHasPermission(PERMISSIONS.exportTaskView)
  const canViewOperations = useHasPermission(PERMISSIONS.systemAudit)
  const tasksEnabled = Boolean(user) && canViewTasks
  const { data: summary, isError: isSummaryError, refetch: refetchTasks } = useExportTasks(tasksEnabled)
  const handlers = useDownloadTaskHandlers(() => setOpen(false))
  useExportTaskEvents(tasksEnabled)

  useEffect(() => {
    const openFromNotification = () => {
      setOpen(true)
      if (tasksEnabled) void refetchTasks()
    }
    window.addEventListener(OPEN_DOWNLOAD_TASK_CENTER_EVENT, openFromNotification)
    return () => window.removeEventListener(OPEN_DOWNLOAD_TASK_CENTER_EVENT, openFromNotification)
  }, [refetchTasks, tasksEnabled])

  if (!canViewTasks && !canViewOperations) return null
  const showCenter = () => {
    setOpen(true)
    if (canViewTasks) void refetchTasks()
  }

  return <>
    <DownloadTaskCenterTrigger runningCount={summary?.runningCount ?? 0}
      unacknowledgedCount={summary?.unacknowledgedCount ?? 0}
      error={isSummaryError} onClick={showCenter} />
    <Drawer title="下载任务中心" width={460} open={open} rootClassName="download-task-center"
      onClose={() => setOpen(false)}>
      <DownloadTaskDrawerContent canViewTasks={canViewTasks} canViewOperations={canViewOperations}
        open={open} handlers={handlers} unacknowledgedCount={summary?.unacknowledgedCount ?? 0} />
    </Drawer>
  </>
}

interface TriggerProps {
  runningCount: number
  unacknowledgedCount: number
  error: boolean
  onClick: () => void
}

export function DownloadTaskCenterTrigger(props: TriggerProps) {
  const { error, onClick, runningCount, unacknowledgedCount } = props
  const statusText = `进行中 ${runningCount} 个，待处理 ${unacknowledgedCount} 个`
  const title = error ? '下载任务状态加载失败，点击重试' : `下载任务中心：${statusText}`
  const stateClass = error ? ' is-error' : runningCount > 0 ? ' is-running' : ''
  const label = error ? '下载任务中心，任务状态加载失败' : `下载任务中心，${statusText}`
  return <Tooltip title={title}>
    <Badge count={unacknowledgedCount} dot={error} size="small" overflowCount={99}>
      <Button type="text" className={`download-task-center__trigger${stateClass}`}
        icon={<CloudDownloadOutlined />}
        aria-label={label} onClick={onClick} />
    </Badge>
  </Tooltip>
}
