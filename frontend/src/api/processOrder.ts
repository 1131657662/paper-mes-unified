import request, { rawRequest } from './request'
import type { PageResult } from '../types/common'
import { downloadFileFromResponse } from '../utils/downloadFile'
import {
  normalizeDocumentExportInput,
  readableExportFilename,
  type DocumentExportInput,
} from '../utils/documentExport'
import type {
  BackRecordDTO,
  BackRecordResultVO,
  DraftOrderVO,
  DraftOrderBaseDTO,
  DraftProgressDTO,
  DraftSummaryVO,
  FeeResultVO,
  FinishConfigSaveDTO,
  FinishConfigSaveVO,
  FinishPreviewVO,
  FinishRollBatchDTO,
  OriginalRollImportPreviewVO,
  OriginalRollBatchSaveDTO,
  OriginalRollDTO,
  OriginalRollRemarkDTO,
  PlanPreviewVO,
  PrintDTO,
  PrintResultVO,
  ProcessConfigDraftSaveDTO,
  ProcessOrder,
  ProcessOrderCreateDTO,
  ProcessOrderDetailVO,
  ProcessOrderQuery,
  ProcessOrderRemarkDTO,
  ProcessOrderRollbackDTO,
  ProcessOrderVoidDTO,
  ProcessOrderSubmitVO,
  ProcessPlanBatchSaveDTO,
  ProcessPlanPreviewRequestDTO,
  ProcessRoutePreviewDTO,
  ProcessRoutePreviewVO,
  RewindPlanPreviewDTO,
  SnapshotDiffVO,
  SpareRollAppendDTO,
  SpareRollBatchVoidDTO,
  StatusChangeDTO,
} from '../types/processOrder'

export function pageProcessOrders(query: ProcessOrderQuery) {
  return request<PageResult<ProcessOrder>>({
    url: '/api/process-orders',
    method: 'get',
    params: query,
  })
}

export function getProcessOrder(uuid: string) {
  return request<ProcessOrderDetailVO>({
    url: `/api/process-orders/${uuid}`,
    method: 'get',
  })
}

export async function exportProcessOrderDetail(input: DocumentExportInput) {
  const { documentNo, uuid } = normalizeDocumentExportInput(input)
  const response = await rawRequest.request<Blob, { data: Blob; headers: Record<string, string> }>({
    url: `/api/process-orders/${uuid}/export`,
    method: 'get',
    responseType: 'blob',
  })
  await downloadFileFromResponse(response, readableExportFilename('加工单资料', documentNo))
}

export function createProcessOrder(dto: ProcessOrderCreateDTO) {
  return request<string>({ url: '/api/process-orders', method: 'post', data: dto })
}

export function createProcessOrderDraft(dto: DraftOrderBaseDTO) {
  return request<string>({ url: '/api/process-orders/drafts', method: 'post', data: dto })
}

export function listProcessOrderDrafts() {
  return request<DraftSummaryVO[]>({ url: '/api/process-orders/drafts', method: 'get' })
}

export function getProcessOrderDraft(uuid: string) {
  return request<DraftOrderVO>({ url: `/api/process-orders/${uuid}/draft`, method: 'get' })
}

export function saveDraftBaseInfo(uuid: string, dto: DraftOrderBaseDTO) {
  return request<void>({ url: `/api/process-orders/${uuid}/base-info`, method: 'put', data: dto })
}

export function saveDraftProgress(uuid: string, dto: DraftProgressDTO) {
  return request<void>({
    url: `/api/process-orders/${uuid}/draft-progress`,
    method: 'put',
    data: dto,
  })
}

export function replaceDraftOriginalRolls(uuid: string, dto: OriginalRollBatchSaveDTO) {
  return request<string[]>({
    url: `/api/process-orders/${uuid}/original-rolls`,
    method: 'put',
    data: dto,
  })
}

export function previewOriginalRollImport(uuid: string, file: File) {
  const form = new FormData()
  form.append('file', file)
  return request<OriginalRollImportPreviewVO>({
    url: `/api/process-orders/${uuid}/original-rolls/import-preview`,
    method: 'post',
    data: form,
  })
}

export function updateOriginalRoll(rollUuid: string, dto: OriginalRollDTO) {
  return request<void>({ url: `/api/process-orders/rolls/${rollUuid}`, method: 'put', data: dto })
}

export function updateProcessOrderRemark(uuid: string, dto: ProcessOrderRemarkDTO) {
  return request<void>({ url: `/api/process-orders/${uuid}/remarks`, method: 'put', data: dto })
}

export function updateOriginalRollRemark(rollUuid: string, dto: OriginalRollRemarkDTO) {
  return request<void>({ url: `/api/process-orders/rolls/${rollUuid}/remarks`, method: 'put', data: dto })
}

export function saveProcessConfigDraft(
  orderUuid: string,
  rollUuid: string,
  dto: ProcessConfigDraftSaveDTO,
) {
  return request<void>({
    url: `/api/process-orders/${orderUuid}/rolls/${rollUuid}/process-config`,
    method: 'put',
    data: dto,
  })
}

export function previewProcessPlan(orderUuid: string, dto: ProcessPlanPreviewRequestDTO) {
  return request<PlanPreviewVO>({
    url: `/api/process-orders/${orderUuid}/rolls/plan-preview`,
    method: 'post',
    data: dto,
  })
}

export function previewProcessRoute(orderUuid: string, dto: ProcessRoutePreviewDTO) {
  return request<ProcessRoutePreviewVO>({
    url: `/api/process-orders/${orderUuid}/rolls/route-preview`,
    method: 'post',
    data: dto,
  })
}

export function saveDraftProcessRoute(orderUuid: string, dto: ProcessRoutePreviewDTO) {
  if (!dto.originalUuid) {
    return Promise.reject(new Error('缺少母卷编号，无法保存链式工艺'))
  }
  return request<ProcessRoutePreviewVO>({
    url: `/api/process-orders/${orderUuid}/rolls/${dto.originalUuid}/route-plan`,
    method: 'put',
    data: dto,
  })
}

export function previewPendingProcessRoute(orderUuid: string, dto: ProcessRoutePreviewDTO) {
  return request<ProcessRoutePreviewVO>({
    url: `/api/process-orders/${orderUuid}/route-preview`,
    method: 'post',
    data: dto,
  })
}

export function savePendingProcessRoute(orderUuid: string, dto: ProcessRoutePreviewDTO) {
  return request<ProcessRoutePreviewVO>({
    url: `/api/process-orders/${orderUuid}/route-config`,
    method: 'post',
    data: dto,
  })
}

export function previewAppendProcessRoute(orderUuid: string, dto: ProcessRoutePreviewDTO) {
  return request<ProcessRoutePreviewVO>({
    url: `/api/process-orders/${orderUuid}/route-append-preview`,
    method: 'post',
    data: dto,
  })
}

export function saveAppendProcessRoute(orderUuid: string, dto: ProcessRoutePreviewDTO) {
  return request<ProcessRoutePreviewVO>({
    url: `/api/process-orders/${orderUuid}/route-append`,
    method: 'post',
    data: dto,
  })
}

export function saveProcessPlan(orderUuid: string, rollUuid: string, dto: ProcessPlanPreviewRequestDTO) {
  return request<PlanPreviewVO>({
    url: `/api/process-orders/${orderUuid}/rolls/${rollUuid}/process-plan`,
    method: 'put',
    data: dto,
  })
}

export function saveProcessPlanBatch(orderUuid: string, dto: ProcessPlanBatchSaveDTO) {
  return request<PlanPreviewVO[]>({
    url: `/api/process-orders/${orderUuid}/rolls/process-plan/batch`,
    method: 'put',
    data: dto,
  })
}

export function submitProcessOrderDraft(uuid: string) {
  return request<ProcessOrderSubmitVO>({
    url: `/api/process-orders/${uuid}/submit`,
    method: 'post',
  })
}

export function changeOrderStatus(uuid: string, dto: StatusChangeDTO) {
  return request<void>({
    url: `/api/process-orders/${uuid}/status`,
    method: 'put',
    data: dto,
  })
}

export function rollbackProcessOrderToDraft(uuid: string, dto: ProcessOrderRollbackDTO) {
  return request<void>({
    url: `/api/process-orders/${uuid}/rollback-draft`,
    method: 'put',
    data: dto,
  })
}

export function voidProcessOrder(uuid: string, dto: ProcessOrderVoidDTO) {
  return request<void>({
    url: `/api/process-orders/${uuid}/void`,
    method: 'put',
    data: dto,
  })
}

export function printProcessOrder(uuid: string, dto?: PrintDTO) {
  return request<PrintResultVO>({
    url: `/api/process-orders/${uuid}/print`,
    method: 'post',
    data: dto,
  })
}

export function calcProcessOrderFee(uuid: string) {
  return request<FeeResultVO>({
    url: `/api/process-orders/${uuid}/calc-fee`,
    method: 'post',
  })
}

export function getSnapshotDiff(uuid: string) {
  return request<SnapshotDiffVO>({
    url: `/api/process-orders/${uuid}/snapshot-diff`,
    method: 'get',
  })
}

export function backRecordProcessOrder(uuid: string, dto: BackRecordDTO) {
  return request<BackRecordResultVO>({
    url: `/api/process-orders/${uuid}/back-record`,
    method: 'post',
    data: dto,
  })
}

export function saveFinishConfig(orderUuid: string, rollUuid: string, dto: FinishConfigSaveDTO) {
  return request<FinishConfigSaveVO>({
    url: `/api/process-orders/${orderUuid}/rolls/${rollUuid}/finish-config`,
    method: 'post',
    data: dto,
  })
}

export function previewRewindPlan(orderUuid: string, rollUuid: string, dto: RewindPlanPreviewDTO) {
  return request<FinishPreviewVO>({
    url: `/api/process-orders/${orderUuid}/rolls/${rollUuid}/rewind-plan/preview`,
    method: 'post',
    data: dto,
  })
}

export function batchGenerateFinishRolls(orderUuid: string, dto: FinishRollBatchDTO) {
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
    url: `/api/finish-rolls/batch-void`,
    method: 'post',
    data: dto,
  })
}

export function checkRollNoAvailable(rollNo: string, excludeUuid?: string) {
  return request<boolean>({
    url: `/api/finish-rolls/check`,
    method: 'get',
    params: { rollNo, excludeUuid },
  })
}

// ==================== Phase 5.1：追加工序功能 ====================

export interface ProcessStepDTO {
  originalUuid: string
  stepType: number
  stepName?: string
  isMain?: number
  knifeCount?: number
  processWeight?: number
  unitPrice?: number
  remark?: string
}

/** 新增工序 */
export function addProcessStep(orderUuid: string, data: ProcessStepDTO) {
  return request<void>({
    url: `/api/process-orders/${orderUuid}/steps`,
    method: 'post',
    data,
  })
}

/** 修改工序 */
export function updateProcessStep(stepUuid: string, data: ProcessStepDTO) {
  return request<void>({
    url: `/api/process-orders/steps/${stepUuid}`,
    method: 'put',
    data,
  })
}

/** 删除工序 */
export function deleteProcessStep(stepUuid: string) {
  return request<void>({
    url: `/api/process-orders/steps/${stepUuid}`,
    method: 'delete',
  })
}
