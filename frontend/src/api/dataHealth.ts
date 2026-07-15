import request from './request'
import type {
  DataHealthRepairRequest,
  DataHealthRepairResult,
  DataHealthSummary,
} from '../types/dataHealth'

export function getDataHealthSummary(): Promise<DataHealthSummary> {
  return request<DataHealthSummary>({ url: '/api/system/data-health', method: 'get' })
}

export function reconcileSettlement(
  uuid: string,
  data: DataHealthRepairRequest,
): Promise<DataHealthRepairResult> {
  return request<DataHealthRepairResult>({
    url: `/api/system/data-health/settlements/${uuid}/reconcile`, method: 'post', data,
  })
}

export function restoreCompletedOrder(
  uuid: string,
  data: DataHealthRepairRequest,
): Promise<DataHealthRepairResult> {
  return request<DataHealthRepairResult>({
    url: `/api/system/data-health/process-orders/${uuid}/restore-completed`, method: 'post', data,
  })
}
