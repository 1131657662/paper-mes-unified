import request from './request'
import type { FeeResultVO } from '../types/processOrder'

export interface ProcessStepDTO {
  expectedVersion?: number
  originalUuid: string
  stepType: number
  stepName?: string
  machineUuid?: string
  isMain?: number
  knifeCount?: number
  processWeight?: number
  billingBasis?: string
  serviceQuantity?: number
  billingMode?: 1 | 2 | 3 | 4
  billingAmount?: number
  unitPrice?: number
  remark?: string
}

export interface ProcessStepBatchDTO {
  expectedVersion?: number
  steps: ProcessStepDTO[]
}

export interface ProcessStepBatchResult {
  selectedCount: number
  createdCount: number
  updatedCount: number
  recovered?: boolean
}

export interface ProcessStepPricingAdjustmentDTO {
  /** 1标准计价 2指定数量 3固定金额 4免收 */
  billingMode: number
  billingQuantity?: number
  billingAmount?: number
  billingBasis?: 'PIECE' | 'TON'
  billingUnitPrice?: number
  reason: string
}

export interface ProcessStepPricingBatchGroupDTO {
  stepType: 1 | 2 | 3 | 4
  stepUuids: string[]
  restoreStandard: boolean
  billingUnitPrice?: number
  billingMode?: 1 | 3 | 4
  billingBasis?: 'PIECE' | 'TON'
  billingAmount?: number
}

export interface ProcessStepPricingBatchDTO {
  expectedOrderVersion: number
  reason: string
  requestId?: string
  groups: ProcessStepPricingBatchGroupDTO[]
}

export interface ProcessStepPricingBatchPreviewRow {
  stepUuid: string
  originalUuid?: string
  stepType: 1 | 2 | 3 | 4
  stepName?: string
  billingMode: 1 | 2 | 3 | 4
  billingBasis?: 'PIECE' | 'TON'
  quantity: number
  standardUnitPrice: number
  currentUnitPrice: number
  finalUnitPrice: number
  standardAmount: number
  currentAmount: number
  finalAmount: number
  adjustmentAmount: number
}

export interface ProcessStepPricingBatchPreviewVO {
  orderUuid: string
  orderNo: string
  orderVersion: number
  stepCount: number
  standardAmount: number
  currentAmount: number
  finalAmount: number
  adjustmentAmount: number
  rows: ProcessStepPricingBatchPreviewRow[]
}

export function addProcessStep(orderUuid: string, data: ProcessStepDTO): Promise<void> {
  return request<void>({ url: `/api/process-orders/${orderUuid}/steps`, method: 'post', data })
}

export function addProcessStepsBatch(orderUuid: string, data: ProcessStepBatchDTO): Promise<ProcessStepBatchResult> {
  return request<ProcessStepBatchResult>({ url: `/api/process-orders/${orderUuid}/steps/batch`, method: 'post', data })
}

export function addDraftProcessStep(orderUuid: string, data: ProcessStepDTO, expectedVersion: number): Promise<void> {
  return request<void>({
    url: `/api/process-orders/${orderUuid}/draft-steps`,
    method: 'post',
    data: { ...data, expectedVersion },
    deferUncertainErrorNotification: true,
  })
}

export function addDraftProcessStepsBatch(
  orderUuid: string,
  data: ProcessStepBatchDTO,
  expectedVersion: number,
): Promise<ProcessStepBatchResult> {
  return request<ProcessStepBatchResult>({
    url: `/api/process-orders/${orderUuid}/draft-steps/batch`,
    method: 'post',
    data: { ...data, expectedVersion },
    deferUncertainErrorNotification: true,
  })
}

export function updateDraftProcessStep(stepUuid: string, data: ProcessStepDTO, expectedVersion: number): Promise<void> {
  return request<void>({
    url: `/api/process-orders/draft-steps/${stepUuid}`,
    method: 'put',
    data: { ...data, expectedVersion },
    deferUncertainErrorNotification: true,
  })
}

export function deleteDraftProcessStep(stepUuid: string, expectedVersion: number): Promise<void> {
  return request<void>({
    url: `/api/process-orders/draft-steps/${stepUuid}`,
    method: 'delete',
    params: { expectedVersion },
    deferUncertainErrorNotification: true,
  })
}

export function updateProcessStep(stepUuid: string, data: ProcessStepDTO): Promise<void> {
  return request<void>({ url: `/api/process-orders/steps/${stepUuid}`, method: 'put', data })
}

export function deleteProcessStep(stepUuid: string): Promise<void> {
  return request<void>({ url: `/api/process-orders/steps/${stepUuid}`, method: 'delete' })
}

export function adjustProcessStepPricing(stepUuid: string, data: ProcessStepPricingAdjustmentDTO): Promise<FeeResultVO> {
  return request<FeeResultVO>({ url: `/api/process-orders/steps/${stepUuid}/pricing`, method: 'put', data })
}

export function previewProcessStepPricingBatch(orderUuid: string, data: ProcessStepPricingBatchDTO): Promise<ProcessStepPricingBatchPreviewVO> {
  return request<ProcessStepPricingBatchPreviewVO>({
    url: `/api/process-orders/${orderUuid}/pricing-adjustments/preview`,
    method: 'post',
    data,
  })
}

export function applyProcessStepPricingBatch(orderUuid: string, data: ProcessStepPricingBatchDTO): Promise<ProcessStepPricingBatchPreviewVO> {
  return request<ProcessStepPricingBatchPreviewVO>({
    url: `/api/process-orders/${orderUuid}/pricing-adjustments`,
    method: 'put',
    data,
  })
}
