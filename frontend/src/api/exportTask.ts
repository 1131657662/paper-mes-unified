import request, { rawRequest } from './request'
import type {
  ExportTaskAcknowledgeFilter,
  ExportTaskHistoryQuery,
  ExportTaskHistoryPage,
  ExportTaskOperations,
  ExportTaskOperationsIssues,
  ExportTaskSummary,
} from '../types/exportTask'
import type { DeliveryInventoryFinishQuery } from '../types/deliveryInventory'
import type { DeliveryQuery } from '../types/delivery'
import type { ReportQuery } from '../types/report'
import { downloadFileFromResponse } from '../utils/downloadFile'

export function getExportTaskSummary() {
  return request<ExportTaskSummary>({ url: '/api/export-tasks', method: 'get' })
}

export function getExportTaskOperations() {
  return request<ExportTaskOperations>({ url: '/api/export-tasks/operations', method: 'get', silentError: true })
}

export function getExportTaskOperationsIssues() {
  return request<ExportTaskOperationsIssues>({
    url: '/api/export-tasks/operations/issues', method: 'get', silentError: true,
  })
}

export function getExportTaskHistory(query: ExportTaskHistoryQuery) {
  return request<ExportTaskHistoryPage>({
    url: '/api/export-tasks/history', method: 'get', params: query, silentError: true,
  })
}

export function createSettleExportTask(uuid: string, requestId: string) {
  return request<string>({ url: `/api/export-tasks/settle-orders/${uuid}`, method: 'post', data: { requestId } })
}

export function createProcessOrderExportTask(uuid: string, requestId: string = crypto.randomUUID()) {
  return request<string>({ url: `/api/export-tasks/process-orders/${uuid}`, method: 'post', data: { requestId } })
}

export function createDeliveryOrderExportTask(uuid: string, requestId: string = crypto.randomUUID()) {
  return request<string>({ url: `/api/export-tasks/delivery-orders/${uuid}`, method: 'post', data: { requestId } })
}

export function createDeliveryInventoryExportTask(
  query: DeliveryInventoryFinishQuery,
  requestId = crypto.randomUUID(),
) {
  return request<string>({
    url: '/api/export-tasks/delivery-inventory', method: 'post',
    data: { requestId, query },
  })
}

export function createDeliveryReconciliationExportTask(
  query: DeliveryQuery,
  requestId = crypto.randomUUID(),
) {
  return request<string>({
    url: '/api/export-tasks/delivery-reconciliation', method: 'post',
    data: { requestId, query },
  })
}

export function createReportExportTask(query: ReportQuery, requestId = crypto.randomUUID()) {
  return request<string>({
    url: '/api/export-tasks/reports', method: 'post',
    data: { requestId, query },
  })
}

export function acknowledgeExportTasks(filter: ExportTaskAcknowledgeFilter) {
  return request<number>({ url: '/api/export-tasks/acknowledge', method: 'put', data: filter })
}

export function retryExportTask(uuid: string) {
  return request<void>({ url: `/api/export-tasks/${uuid}/retry`, method: 'post' })
}

export function cancelExportTask(uuid: string) {
  return request<void>({ url: `/api/export-tasks/${uuid}/cancel`, method: 'post' })
}

export function acknowledgeExportTask(uuid: string) {
  return request<void>({ url: `/api/export-tasks/${uuid}/acknowledge`, method: 'put' })
}

export async function downloadExportTask(uuid: string, filename?: string) {
  const response = await rawRequest.request<Blob, { data: Blob; headers: Record<string, string> }>({
    url: `/api/export-tasks/${uuid}/download`,
    method: 'get',
    responseType: 'blob',
  })
  return downloadFileFromResponse(response, filename || '导出文件.xlsx')
}
