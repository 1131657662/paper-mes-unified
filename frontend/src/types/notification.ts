export interface SystemNotification {
  uuid: string
  notificationType: string
  severity: 'ERROR' | 'WARNING' | 'INFO'
  title: string
  content: string
  sourceType: string
  sourceUuid: string
  read: boolean
  createdAt: string
}

export interface NotificationSummary {
  unreadCount: number
  items: SystemNotification[]
}
