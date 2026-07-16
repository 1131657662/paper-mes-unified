import {
  cancelReceivePayment,
  createSettleByMonth,
  createSettleByOrders,
  exportSettleOrder,
  getSettleCandidates,
  getSettleOrderDetail,
  getSettleOrderList,
  getSettleOrderSummary,
  receivePayment,
  quoteSettleByMonth,
  quoteSettleByOrders,
  voidSettleOrder,
  getSettleDiscountApprovals,
  requestSettleDiscountApproval,
  approveSettleDiscount,
} from '../../../api/settle'
import type {
  ReceiveDTO,
  SettleActionReasonDTO,
  SettleByMonthDTO,
  SettleByOrdersDTO,
  SettleCandidateQuery,
  SettleQuery,
  SettleQuoteByOrdersDTO,
  SettleQuoteByMonthDTO,
  SettleDiscountApprovalRequestDTO,
} from '../../../types/settle'
import type { DocumentExportInput } from '../../../utils/documentExport'

export const settleService = {
  candidates: (query: SettleCandidateQuery) => getSettleCandidates(query),
  quoteByMonth: (data: SettleQuoteByMonthDTO) => quoteSettleByMonth(data),
  quoteByOrders: (data: SettleQuoteByOrdersDTO) => quoteSettleByOrders(data),
  createByMonth: (data: SettleByMonthDTO) => createSettleByMonth(data),
  createByOrders: (data: SettleByOrdersDTO) => createSettleByOrders(data),
  detail: (uuid: string) => getSettleOrderDetail(uuid),
  export: (params: DocumentExportInput) => exportSettleOrder(params),
  list: (query: SettleQuery) => getSettleOrderList(query),
  summary: (query: SettleQuery) => getSettleOrderSummary(query),
  receive: (params: { uuid: string; data: ReceiveDTO }) => receivePayment(params.uuid, params.data),
  discountApprovals: (uuid: string) => getSettleDiscountApprovals(uuid),
  requestDiscountApproval: (params: { uuid: string; data: SettleDiscountApprovalRequestDTO }) =>
    requestSettleDiscountApproval(params.uuid, params.data),
  approveDiscount: (params: { uuid: string; approvalUuid: string }) =>
    approveSettleDiscount(params.uuid, params.approvalUuid),
  cancelReceive: (params: { uuid: string; receiveUuid: string; data: SettleActionReasonDTO }) =>
    cancelReceivePayment(params.uuid, params.receiveUuid, params.data),
  void: (params: { uuid: string; data: SettleActionReasonDTO }) => voidSettleOrder(params.uuid, params.data),
}
