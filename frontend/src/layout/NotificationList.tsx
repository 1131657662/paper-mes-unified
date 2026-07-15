import { AlertOutlined, CheckOutlined, DatabaseOutlined, InfoCircleOutlined } from '@ant-design/icons'
import { Button, Empty, List, Tag, Typography } from 'antd'
import dayjs from 'dayjs'
import type { SystemNotification } from '../types/notification'

interface NotificationListProps {
  items: SystemNotification[]
  markingAll: boolean
  unreadCount: number
  onMarkAllRead: () => void
  onOpen: (item: SystemNotification) => void
}

export default function NotificationList(props: NotificationListProps) {
  if (props.items.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无通知" />
  }
  return (
    <>
      <div className="notification-center__toolbar">
        <Typography.Text type="secondary">最近 20 条</Typography.Text>
        <Button
          type="text"
          size="small"
          icon={<CheckOutlined />}
          disabled={props.unreadCount === 0}
          loading={props.markingAll}
          onClick={props.onMarkAllRead}
        >
          全部已读
        </Button>
      </div>
      <List
        className="notification-center__list"
        dataSource={props.items}
        renderItem={(item) => (
          <List.Item className={item.read ? '' : 'notification-center__row--unread'}>
            <button
              type="button"
              className="notification-center__item"
              onClick={() => props.onOpen(item)}
            >
              <NotificationIcon severity={item.severity} />
              <span className="notification-center__body">
                <span className="notification-center__title-line">
                  <Typography.Text strong={!item.read}>{item.title}</Typography.Text>
                  <Tag color={severityColor(item.severity)}>{severityLabel(item.severity)}</Tag>
                </span>
                <Typography.Text type="secondary" className="notification-center__content">
                  {item.content}
                </Typography.Text>
                <Typography.Text type="secondary" className="notification-center__time">
                  {dayjs(item.createdAt).format('YYYY-MM-DD HH:mm:ss')}
                </Typography.Text>
              </span>
              {!item.read && <span className="notification-center__dot" aria-label="未读" />}
            </button>
          </List.Item>
        )}
      />
    </>
  )
}

function NotificationIcon({ severity }: { severity: SystemNotification['severity'] }) {
  const Icon = severity === 'ERROR'
    ? AlertOutlined
    : severity === 'WARNING'
      ? DatabaseOutlined
      : InfoCircleOutlined
  return <span className={`notification-center__icon notification-center__icon--${severity.toLowerCase()}`}><Icon /></span>
}

function severityColor(severity: SystemNotification['severity']) {
  if (severity === 'ERROR') return 'error'
  if (severity === 'WARNING') return 'warning'
  return 'processing'
}

function severityLabel(severity: SystemNotification['severity']) {
  if (severity === 'ERROR') return '严重'
  if (severity === 'WARNING') return '警告'
  return '提示'
}
