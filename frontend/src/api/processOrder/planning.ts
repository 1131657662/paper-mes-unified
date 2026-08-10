import request from '../request'
import type {
  PlanPreviewVO,
  ProcessConfigDraftSaveDTO,
  ProcessPlanBatchSaveDTO,
  ProcessPlanItemsBatchSaveDTO,
  ProcessPlanPreviewRequestDTO,
  ProcessRouteBatchSaveDTO,
  ProcessRoutePreviewDTO,
  ProcessRoutePreviewVO,
} from '../../types/processOrder'

export function saveProcessConfigDraft(
  orderUuid: string,
  rollUuid: string,
  dto: ProcessConfigDraftSaveDTO,
) {
  return request<void>({
    url: `/api/process-orders/${orderUuid}/rolls/${rollUuid}/process-config`,
    method: 'put',
    data: dto,
    deferUncertainErrorNotification: true,
  })
}

export function previewProcessPlan(
  orderUuid: string,
  dto: ProcessPlanPreviewRequestDTO,
  signal?: AbortSignal,
) {
  return request<PlanPreviewVO>({
    url: `/api/process-orders/${orderUuid}/rolls/plan-preview`,
    method: 'post',
    data: dto,
    signal,
    silentError: true,
  })
}

export function previewProcessRoute(
  orderUuid: string,
  dto: ProcessRoutePreviewDTO,
) {
  return request<ProcessRoutePreviewVO>({
    url: `/api/process-orders/${orderUuid}/rolls/route-preview`,
    method: 'post',
    data: dto,
  })
}

export function saveDraftProcessRoute(
  orderUuid: string,
  dto: ProcessRoutePreviewDTO,
) {
  if (!dto.originalUuid) {
    return Promise.reject(new Error('缺少母卷编号，无法保存链式工艺'))
  }
  return request<ProcessRoutePreviewVO>({
    url: `/api/process-orders/${orderUuid}/rolls/${dto.originalUuid}/route-plan`,
    method: 'put',
    data: dto,
    deferUncertainErrorNotification: true,
  })
}

export function saveDraftProcessRouteBatch(
  orderUuid: string,
  dto: ProcessRouteBatchSaveDTO,
) {
  return request<ProcessRoutePreviewVO[]>({
    url: `/api/process-orders/${orderUuid}/rolls/route-plan/batch`,
    method: 'put',
    data: dto,
    deferUncertainErrorNotification: true,
  })
}

export function previewPendingProcessRoute(
  orderUuid: string,
  dto: ProcessRoutePreviewDTO,
) {
  return request<ProcessRoutePreviewVO>({
    url: `/api/process-orders/${orderUuid}/route-preview`,
    method: 'post',
    data: dto,
  })
}

export function savePendingProcessRoute(
  orderUuid: string,
  dto: ProcessRoutePreviewDTO,
) {
  return request<ProcessRoutePreviewVO>({
    url: `/api/process-orders/${orderUuid}/route-config`,
    method: 'post',
    data: dto,
  })
}

export function previewAppendProcessRoute(
  orderUuid: string,
  dto: ProcessRoutePreviewDTO,
) {
  return request<ProcessRoutePreviewVO>({
    url: `/api/process-orders/${orderUuid}/route-append-preview`,
    method: 'post',
    data: dto,
  })
}

export function saveAppendProcessRoute(
  orderUuid: string,
  dto: ProcessRoutePreviewDTO,
) {
  return request<ProcessRoutePreviewVO>({
    url: `/api/process-orders/${orderUuid}/route-append`,
    method: 'post',
    data: dto,
  })
}

export function saveProcessPlan(
  orderUuid: string,
  rollUuid: string,
  dto: ProcessPlanPreviewRequestDTO,
) {
  return request<PlanPreviewVO>({
    url: `/api/process-orders/${orderUuid}/rolls/${rollUuid}/process-plan`,
    method: 'put',
    data: dto,
    deferUncertainErrorNotification: true,
  })
}

export function saveProcessPlanBatch(
  orderUuid: string,
  dto: ProcessPlanBatchSaveDTO,
) {
  return request<PlanPreviewVO[]>({
    url: `/api/process-orders/${orderUuid}/rolls/process-plan/batch`,
    method: 'put',
    data: dto,
    deferUncertainErrorNotification: true,
  })
}

export function saveProcessPlanItemsBatch(
  orderUuid: string,
  dto: ProcessPlanItemsBatchSaveDTO,
) {
  return request<PlanPreviewVO[]>({
    url: `/api/process-orders/${orderUuid}/rolls/process-plan/items-batch`,
    method: 'put',
    data: dto,
    deferUncertainErrorNotification: true,
  })
}
