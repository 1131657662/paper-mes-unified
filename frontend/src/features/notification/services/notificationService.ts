import {
  getNotificationSummary,
  markAllNotificationsRead,
  markNotificationRead,
} from '../../../api/notification'

export const notificationService = {
  summary: getNotificationSummary,
  markRead: markNotificationRead,
  markAllRead: markAllNotificationsRead,
}
