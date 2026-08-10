import request from '../request'
import type {
  FinishConfigBatchSaveDTO,
  FinishConfigBatchSaveVO,
  FinishConfigSaveDTO,
  FinishConfigSaveVO,
  FinishPreviewVO,
  FinishRollBatchDTO,
  RewindPlanPreviewDTO,
  SpareRollAppendDTO,
  SpareRollBatchVoidDTO,
} from '../../types/processOrder'

export function saveFinishConfig(
  orderUuid: string,
  rollUuid: string,
  dto: FinishConfigSaveDTO,
  expectedVersion: number,
) {
  return request<FinishConfigSaveVO>({
    url: `/api/process-orders/${orderUuid}/rolls/${rollUuid}/finish-config`,
    method: 'post',
    params: { expectedVersion },
    data: dto,
  })
}

export function saveFinishConfigBatch(
  orderUuid: string,
  dto: FinishConfigBatchSaveDTO,
  expectedVersion: number,
) {
  return request<FinishConfigBatchSaveVO>({
    url: `/api/process-orders/${orderUuid}/finish-config/batch`,
    method: 'post',
    params: { expectedVersion },
    data: dto,
  })
}

export function previewRewindPlan(
  orderUuid: string,
  rollUuid: string,
  dto: RewindPlanPreviewDTO,
) {
  return request<FinishPreviewVO>({
    url: `/api/process-orders/${orderUuid}/rolls/${rollUuid}/rewind-plan/preview`,
    method: 'post',
    data: dto,
  })
}

export function batchGenerateFinishRolls(
  orderUuid: string,
  dto: FinishRollBatchDTO,
) {
  return request<string[]>({
    url: `/api/finish-rolls/orders/${orderUuid}/batch`,
    method: 'post',
    data: dto,
  })
}

export function appendSpareRolls(orderUuid: string, dto: SpareRollAppendDTO) {
  return request<string[]>({
    url: `/api/finish-rolls/orders/${orderUuid}/spare`,
    method: 'post',
    data: dto,
  })
}

export function voidFinishRoll(uuid: string) {
  return request<void>({
    url: `/api/finish-rolls/${uuid}/roll-no`,
    method: 'delete',
  })
}

export function batchVoidFinishRolls(dto: SpareRollBatchVoidDTO) {
  return request<void>({
    url: '/api/finish-rolls/batch-void',
    method: 'post',
    data: dto,
  })
}

export function checkRollNoAvailable(rollNo: string, excludeUuid?: string) {
  return request<boolean>({
    url: '/api/finish-rolls/check',
    method: 'get',
    params: { rollNo, excludeUuid },
  })
}
