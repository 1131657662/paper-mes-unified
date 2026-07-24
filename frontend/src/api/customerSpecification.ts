import request from './request'
import type {
  FinishCustomerRevisionPreview,
  FinishCustomerRevisionDetail,
  FinishCustomerRevisionRequest,
  FinishCustomerRevisionSummary,
} from '../features/processOrderCustomerSpec/customerSpecTypes'

const baseUrl = (orderUuid: string) => `/api/process-orders/${orderUuid}/customer-specs`

export function getFinishCustomerSpecs(orderUuid: string) {
  return request<FinishCustomerRevisionPreview>({ url: baseUrl(orderUuid), method: 'get' })
}

export function getFinishCustomerSpecRevisions(orderUuid: string) {
  return request<FinishCustomerRevisionSummary[]>({
    url: `${baseUrl(orderUuid)}/revisions`,
    method: 'get',
  })
}

export function getFinishCustomerSpecRevisionDetail(orderUuid: string, revisionUuid: string) {
  return request<FinishCustomerRevisionDetail>({
    url: `${baseUrl(orderUuid)}/revisions/${revisionUuid}`,
    method: 'get',
  })
}

export function previewFinishCustomerSpecRevision(
  orderUuid: string,
  data: FinishCustomerRevisionRequest,
) {
  return request<FinishCustomerRevisionPreview>({
    url: `${baseUrl(orderUuid)}/preview`,
    method: 'post',
    data,
  })
}

export function publishFinishCustomerSpecRevision(
  orderUuid: string,
  data: FinishCustomerRevisionRequest,
) {
  return request<FinishCustomerRevisionSummary>({
    url: `${baseUrl(orderUuid)}/revisions`,
    method: 'post',
    data,
  })
}
