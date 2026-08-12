import request, { type MesRequestConfig } from '../request'
import type { PageResult } from '../../types/common'
import type {
  DraftOrderBaseDTO,
  DraftOrderVO,
  DraftProgressDTO,
  DraftRollProcessBatchSaveDTO,
  DraftSummaryVO,
  OriginalRollBatchSaveDTO,
  OriginalRollDTO,
  OriginalRollImportPreviewVO,
  OriginalRollRemarkDTO,
  PrintViewVersion,
  ProcessOrder,
  ProcessOrderCreateDTO,
  ProcessOrderDetailVO,
  ProcessOrderPrintViewVO,
  ProcessOrderIssueConsistency,
  ProcessOrderQuery,
  ProcessOrderRemarkDTO,
} from '../../types/processOrder'

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

export function getProcessOrderPrintView(
  uuid: string,
  version: PrintViewVersion,
) {
  return request<ProcessOrderPrintViewVO>({
    url: `/api/process-orders/${uuid}/print-view`,
    method: 'get',
    params: { version },
  })
}

export function getProcessOrderIssueConsistency(uuid: string) {
  return request<ProcessOrderIssueConsistency>({
    url: `/api/process-orders/${uuid}/issue-consistency`,
    method: 'get',
  })
}

export function getHistoricalProcessOrderIssuePrintView(uuid: string, issueVersion: number) {
  return request<ProcessOrderPrintViewVO>({
    url: `/api/process-orders/${uuid}/issue-versions/${issueVersion}/print-view`,
    method: 'get',
  })
}

export function createProcessOrder(dto: ProcessOrderCreateDTO) {
  return request<string>({
    url: '/api/process-orders',
    method: 'post',
    data: dto,
  })
}

export function createProcessOrderDraft(dto: DraftOrderBaseDTO) {
  return request<string>({
    url: '/api/process-orders/drafts',
    method: 'post',
    data: dto,
  })
}

export function listProcessOrderDrafts() {
  return request<DraftSummaryVO[]>({
    url: '/api/process-orders/drafts',
    method: 'get',
  })
}

export function getProcessOrderDraft(
  uuid: string,
  config?: Pick<MesRequestConfig, 'silentError'>,
) {
  return request<DraftOrderVO>({
    url: `/api/process-orders/${uuid}/draft`,
    method: 'get',
    ...config,
  })
}

export function saveDraftBaseInfo(uuid: string, dto: DraftOrderBaseDTO) {
  return request<void>({
    url: `/api/process-orders/${uuid}/base-info`,
    method: 'put',
    data: dto,
  })
}

export function saveDraftProgress(uuid: string, dto: DraftProgressDTO) {
  return request<void>({
    url: `/api/process-orders/${uuid}/draft-progress`,
    method: 'put',
    data: dto,
    deferUncertainErrorNotification: true,
  })
}

export function replaceDraftOriginalRolls(
  uuid: string,
  dto: OriginalRollBatchSaveDTO,
) {
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
  return request<void>({
    url: `/api/process-orders/rolls/${rollUuid}`,
    method: 'put',
    data: dto,
  })
}

export function saveDraftRollProcesses(
  orderUuid: string,
  dto: DraftRollProcessBatchSaveDTO,
) {
  return request<void>({
    url: `/api/process-orders/${orderUuid}/original-rolls/process-settings`,
    method: 'put',
    data: dto,
    deferUncertainErrorNotification: true,
  })
}

export function updateProcessOrderRemark(
  uuid: string,
  dto: ProcessOrderRemarkDTO,
) {
  return request<void>({
    url: `/api/process-orders/${uuid}/remarks`,
    method: 'put',
    data: dto,
  })
}

export function updateProcessOrderPostProductionNote(
  uuid: string,
  dto: import('../../types/processOrder').ProcessOrderPostProductionNoteDTO,
) {
  return request<void>({
    url: `/api/process-orders/${uuid}/post-production-note`,
    method: 'put',
    data: dto,
  })
}

export function updateOriginalRollRemark(
  rollUuid: string,
  dto: OriginalRollRemarkDTO,
) {
  return request<void>({
    url: `/api/process-orders/rolls/${rollUuid}/remarks`,
    method: 'put',
    data: dto,
  })
}
