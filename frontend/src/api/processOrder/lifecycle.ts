import request from '../request'
import type {
  BackRecordDTO,
  BackRecordReopenDTO,
  BackRecordResultVO,
  DraftSubmitDTO,
  FeeResultVO,
  PhysicalReprintDTO,
  PrintDTO,
  PrintResultVO,
  ProcessOrderIssueVersion,
  ProcessOrderReissueDTO,
  ProcessOrderRollbackDTO,
  ProcessOrderSubmitVO,
  ProcessOrderVoidDTO,
  SnapshotDiffVO,
  StatusChangeDTO,
} from '../../types/processOrder'

export function submitProcessOrderDraft(uuid: string, dto: DraftSubmitDTO) {
  return request<ProcessOrderSubmitVO>({
    url: `/api/process-orders/${uuid}/submit`,
    method: 'post',
    data: dto,
  })
}

export function changeOrderStatus(uuid: string, dto: StatusChangeDTO) {
  return request<void>({
    // Kept as a compatibility name for callers; the server accepts only the
    // explicit rollback command for reverse transitions.
    url: `/api/process-orders/${uuid}/rollback`,
    method: 'put',
    data: dto,
  })
}

export function completeProcessOrder(uuid: string, reason?: string) {
  return request<void>({
    url: `/api/process-orders/${uuid}/to-record`,
    method: 'put',
    data: reason ? { reason } : undefined,
  })
}

export function rollbackProcessOrderToDraft(
  uuid: string,
  dto: ProcessOrderRollbackDTO,
) {
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

export function printAndCompleteProcessOrder(uuid: string, dto?: PrintDTO) {
  return request<PrintResultVO>({
    url: `/api/process-orders/${uuid}/print-and-to-record`,
    method: 'post',
    data: dto,
  })
}

export function physicalReprintProcessOrder(
  uuid: string,
  dto: PhysicalReprintDTO,
) {
  return request<PrintResultVO>({
    url: `/api/process-orders/${uuid}/physical-reprint`,
    method: 'post',
    data: dto,
  })
}

export function issueProcessOrder(uuid: string) {
  return request<PrintResultVO>({
    url: `/api/process-orders/${uuid}/issue`,
    method: 'post',
  })
}

export function prepareProcessOrderReissue(
  uuid: string,
  dto: ProcessOrderReissueDTO,
) {
  return request<void>({
    url: `/api/process-orders/${uuid}/reissue`,
    method: 'post',
    data: dto,
  })
}

export function listProcessOrderIssueVersions(uuid: string) {
  return request<ProcessOrderIssueVersion[]>({
    url: `/api/process-orders/${uuid}/issue-versions`,
    method: 'get',
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

export function reopenBackRecordBatch(uuid: string, dto: BackRecordReopenDTO) {
  return request<void>({
    url: `/api/process-orders/${uuid}/back-record/reopen`,
    method: 'post',
    data: dto,
  })
}
