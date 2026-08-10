import request from '../request'
import type {
  PlanPreviewVO,
  ProcessOrderAppendCommitDTO,
  ProcessOrderAppendCommitResult,
  ProcessOrderAppendPlanPreviewDTO,
  ProcessOrderAppendPlanSaveDTO,
  ProcessOrderAppendPreviewDTO,
  ProcessOrderAppendProcessSettingsDTO,
  ProcessOrderAppendRollBatchDTO,
  ProcessOrderAppendSessionCreateDTO,
  ProcessOrderAppendSessionVO,
} from '../../types/processOrder'

export function createProcessOrderAppendSession(
  orderUuid: string,
  dto: ProcessOrderAppendSessionCreateDTO,
) {
  return request<ProcessOrderAppendSessionVO>({
    url: `/api/process-orders/${orderUuid}/append-sessions`,
    method: 'post',
    data: dto,
  })
}

export function getProcessOrderAppendSession(
  orderUuid: string,
  sessionUuid: string,
) {
  return request<ProcessOrderAppendSessionVO>({
    url: `/api/process-orders/${orderUuid}/append-sessions/${sessionUuid}`,
    method: 'get',
  })
}

export function saveProcessOrderAppendRolls(
  orderUuid: string,
  sessionUuid: string,
  dto: ProcessOrderAppendRollBatchDTO,
) {
  return request<ProcessOrderAppendSessionVO>({
    url: `/api/process-orders/${orderUuid}/append-sessions/${sessionUuid}/rolls`,
    method: 'put',
    data: dto,
    silentBusinessErrorCodes: ['E006'],
  })
}

export function saveProcessOrderAppendProcessSettings(
  orderUuid: string,
  sessionUuid: string,
  dto: ProcessOrderAppendProcessSettingsDTO,
) {
  return request<ProcessOrderAppendSessionVO>({
    url: `/api/process-orders/${orderUuid}/append-sessions/${sessionUuid}/rolls/process-settings`,
    method: 'put',
    data: dto,
    silentBusinessErrorCodes: ['E006'],
  })
}

export function saveProcessOrderAppendPlan(
  orderUuid: string,
  sessionUuid: string,
  rollUuid: string,
  dto: ProcessOrderAppendPlanSaveDTO,
) {
  return request<ProcessOrderAppendSessionVO>({
    url: `/api/process-orders/${orderUuid}/append-sessions/${sessionUuid}/rolls/${rollUuid}/process-plan`,
    method: 'put',
    data: dto,
    silentBusinessErrorCodes: ['E006'],
  })
}

export function previewProcessOrderAppendPlan(
  orderUuid: string,
  sessionUuid: string,
  rollUuid: string,
  dto: ProcessOrderAppendPlanPreviewDTO,
  signal?: AbortSignal,
) {
  return request<PlanPreviewVO>({
    url: `/api/process-orders/${orderUuid}/append-sessions/${sessionUuid}/rolls/${rollUuid}/process-plan/preview`,
    method: 'post',
    data: dto,
    signal,
    silentError: true,
  })
}

export function previewProcessOrderAppend(
  orderUuid: string,
  sessionUuid: string,
  dto: ProcessOrderAppendPreviewDTO,
) {
  return request<ProcessOrderAppendSessionVO>({
    url: `/api/process-orders/${orderUuid}/append-sessions/${sessionUuid}/preview`,
    method: 'post',
    data: dto,
    silentBusinessErrorCodes: ['E006'],
  })
}

export function commitProcessOrderAppend(
  orderUuid: string,
  sessionUuid: string,
  dto: ProcessOrderAppendCommitDTO,
) {
  return request<ProcessOrderAppendCommitResult>({
    url: `/api/process-orders/${orderUuid}/append-sessions/${sessionUuid}/commit`,
    method: 'post',
    data: dto,
    silentBusinessErrorCodes: ['E006'],
  })
}

export function cancelProcessOrderAppend(
  orderUuid: string,
  sessionUuid: string,
) {
  return request<void>({
    url: `/api/process-orders/${orderUuid}/append-sessions/${sessionUuid}`,
    method: 'delete',
  })
}
