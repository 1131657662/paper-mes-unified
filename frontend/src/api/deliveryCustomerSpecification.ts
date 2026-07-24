import request from './request'
import type {
  DeliveryCustomerRevisionPreview,
  DeliveryCustomerRevisionDetail,
  DeliveryCustomerRevisionRequest,
  DeliveryCustomerRevisionSummary,
} from '../features/deliveryCustomerSpec/deliveryCustomerSpecTypes'

const baseUrl = (uuid: string) => `/api/delivery-orders/${uuid}/customer-specs`

export function getDeliveryCustomerSpecs(uuid: string) {
  return request<DeliveryCustomerRevisionPreview>({ url: baseUrl(uuid), method: 'get' })
}

export function getDeliveryCustomerSpecRevisions(uuid: string) {
  return request<DeliveryCustomerRevisionSummary[]>({ url: `${baseUrl(uuid)}/revisions`, method: 'get' })
}

export function getDeliveryCustomerSpecRevisionDetail(uuid: string, revisionUuid: string) {
  return request<DeliveryCustomerRevisionDetail>({
    url: `${baseUrl(uuid)}/revisions/${revisionUuid}`,
    method: 'get',
  })
}

export function previewDeliveryCustomerSpecRevision(
  uuid: string, data: DeliveryCustomerRevisionRequest,
) {
  return request<DeliveryCustomerRevisionPreview>({
    url: `${baseUrl(uuid)}/preview`, method: 'post', data,
  })
}

export function publishDeliveryCustomerSpecRevision(
  uuid: string, data: DeliveryCustomerRevisionRequest,
) {
  return request<DeliveryCustomerRevisionSummary>({
    url: `${baseUrl(uuid)}/revisions`, method: 'post', data,
  })
}
