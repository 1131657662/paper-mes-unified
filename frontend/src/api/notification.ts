import request from './request'
import type { NotificationSummary } from '../types/notification'

export function getNotificationSummary() {
  return request<NotificationSummary>({ url: '/api/notifications', method: 'get' })
}

export function markNotificationRead(uuid: string) {
  return request<void>({ url: `/api/notifications/${uuid}/read`, method: 'put' })
}

export function markAllNotificationsRead() {
  return request<void>({ url: '/api/notifications/read-all', method: 'put' })
}
