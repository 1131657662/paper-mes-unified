import { BellOutlined } from '@ant-design/icons'
import { Badge, Button, Drawer, Skeleton, Tooltip } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
} from '../features/notification/hooks/useNotificationMutations'
import { useSystemNotifications } from '../features/notification/hooks/useSystemNotifications'
import { useAuthUser } from '../stores/authStore'
import type { SystemNotification } from '../types/notification'
import NotificationList from './NotificationList'
import './NotificationCenter.css'

export default function NotificationBell() {
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()
  const user = useAuthUser()
  const { data: summary, isLoading: isLoadingNotifications } = useSystemNotifications(Boolean(user))
  const { mutate: markRead } = useMarkNotificationRead()
  const { mutate: markAllRead, isPending: isMarkingAll } = useMarkAllNotificationsRead()
  const unreadCount = summary?.unreadCount ?? 0

  function openNotification(item: SystemNotification) {
    if (!item.read) markRead(item.uuid)
    const path = notificationPath(item)
    if (!path) return
    setOpen(false)
    navigate(path)
  }

  return (
    <>
      <Tooltip title="系统通知">
        <Badge count={unreadCount} size="small" overflowCount={99}>
          <Button
            type="text"
            className="notification-center__trigger"
            icon={<BellOutlined />}
            aria-label={`系统通知，${unreadCount} 条未读`}
            onClick={() => setOpen(true)}
          />
        </Badge>
      </Tooltip>
      <Drawer
        title="系统通知"
        width={400}
        open={open}
        destroyOnHidden
        onClose={() => setOpen(false)}
      >
        {isLoadingNotifications ? (
          <Skeleton active paragraph={{ rows: 5 }} />
        ) : (
          <NotificationList
            items={summary?.items ?? []}
            unreadCount={unreadCount}
            markingAll={isMarkingAll}
            onOpen={openNotification}
            onMarkAllRead={() => markAllRead()}
          />
        )}
      </Drawer>
    </>
  )
}

function notificationPath(item: SystemNotification): string | undefined {
  if (!item.sourceUuid) return undefined
  if (item.sourceType === 'DATA_HEALTH') return '/system-config?section=health'
  if (item.sourceType === 'BACKUP_TASK') {
    const params = new URLSearchParams({ section: 'backup', view: 'tasks', task: item.sourceUuid })
    return `/system-config?${params.toString()}`
  }
  return undefined
}
