import request from './request'
import type {
  ConfirmRemainPriceRequest,
  CreateRemainRegistrationRequest,
  RemainAdjustment,
  RemainAdjustmentCancelRequest,
  RemainAdjustmentCreateRequest,
  RemainAdjustmentNextSettlementRequest,
  RemainCreditRequest,
  RemainCreditReverseRequest,
  RemainInventory,
  RemainInventoryQuery,
  RemainRefund,
  RemainRefundCreateRequest,
  RemainRefundDecisionRequest,
  RemainSale,
  RemainSaleCreateRequest,
  RemainSaleReverseRequest,
  RemainRegistration,
  RemainRegistrationQuery,
  RollbackRemainRequest,
} from '../types/remain'

export const remainApi = {
  registrations(query: RemainRegistrationQuery = {}) {
    return request<RemainRegistration[]>({ url: '/api/remain-registrations', method: 'get', params: query })
  },
  registration(uuid: string) {
    return request<RemainRegistration>({ url: `/api/remain-registrations/${uuid}`, method: 'get' })
  },
  createRegistration(data: CreateRemainRegistrationRequest) {
    return request<RemainRegistration>({ url: '/api/remain-registrations', method: 'post', data })
  },
  confirmPrice(uuid: string, data: ConfirmRemainPriceRequest) {
    return request<RemainRegistration>({ url: `/api/remain-registrations/${uuid}/price`, method: 'post', data })
  },
  rollback(uuid: string, data: RollbackRemainRequest) {
    return request<RemainRegistration>({ url: `/api/remain-registrations/${uuid}/rollback`, method: 'post', data })
  },
  inventory(query: RemainInventoryQuery = {}) {
    return request<RemainInventory[]>({ url: '/api/remain-registrations/inventory', method: 'get', params: query })
  },
  adjustments() {
    return request<RemainAdjustment[]>({ url: '/api/remain-adjustments', method: 'get' })
  },
  adjustment(uuid: string) {
    return request<RemainAdjustment>({ url: `/api/remain-adjustments/${uuid}`, method: 'get' })
  },
  createAdjustment(registrationUuid: string, data: RemainAdjustmentCreateRequest) {
    return request<RemainAdjustment>({ url: `/api/remain-adjustments/registrations/${registrationUuid}`, method: 'post', data })
  },
  cancelAdjustment(uuid: string, data: RemainAdjustmentCancelRequest) {
    return request<RemainAdjustment>({ url: `/api/remain-adjustments/${uuid}/cancel`, method: 'post', data })
  },
  bindNextSettlement(uuid: string, data: RemainAdjustmentNextSettlementRequest) {
    return request<RemainAdjustment>({ url: `/api/remain-adjustments/${uuid}/next-settlement`, method: 'post', data })
  },
  credit(uuid: string, data: RemainCreditRequest) {
    return request<RemainAdjustment>({ url: `/api/remain-adjustments/${uuid}/customer-credit`, method: 'post', data })
  },
  reverseCredit(uuid: string, data: RemainCreditReverseRequest) {
    return request<RemainAdjustment>({ url: `/api/remain-adjustments/${uuid}/customer-credit/reverse`, method: 'post', data })
  },
  refunds() {
    return request<RemainRefund[]>({ url: '/api/remain-refunds', method: 'get' })
  },
  refund(uuid: string) {
    return request<RemainRefund>({ url: `/api/remain-refunds/${uuid}`, method: 'get' })
  },
  createRefund(uuid: string, data: RemainRefundCreateRequest) {
    return request<RemainRefund>({ url: `/api/remain-adjustments/${uuid}/refund`, method: 'post', data })
  },
  approveRefund(uuid: string, data: RemainRefundDecisionRequest) {
    return request<RemainRefund>({ url: `/api/remain-refunds/${uuid}/approve`, method: 'post', data })
  },
  payRefund(uuid: string, data: RemainRefundDecisionRequest) {
    return request<RemainRefund>({ url: `/api/remain-refunds/${uuid}/pay`, method: 'post', data })
  },
  cancelRefund(uuid: string, data: RemainRefundDecisionRequest) {
    return request<RemainRefund>({ url: `/api/remain-refunds/${uuid}/cancel`, method: 'post', data })
  },
  sales() {
    return request<RemainSale[]>({ url: '/api/remain-registrations/sales', method: 'get' })
  },
  createSale(data: RemainSaleCreateRequest) {
    return request<RemainSale>({ url: '/api/remain-registrations/sales', method: 'post', data })
  },
  reverseSale(uuid: string, data: RemainSaleReverseRequest) {
    return request<RemainSale>({ url: `/api/remain-registrations/sales/${uuid}/reverse`, method: 'post', data })
  },
}
