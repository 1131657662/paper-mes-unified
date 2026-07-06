import {
  cancelReceivePayment,
  createSettleByMonth,
  createSettleByOrders,
  exportSettleOrder,
  getSettleCandidates,
  getSettleOrderDetail,
  getSettleOrderList,
  receivePayment,
  voidSettleOrder,
} from '../../../api/settle'
import type {
  ReceiveDTO,
  SettleActionReasonDTO,
  SettleByMonthDTO,
  SettleByOrdersDTO,
  SettleCandidateQuery,
  SettleQuery,
} from '../../../types/settle'
import type { DocumentExportInput } from '../../../utils/documentExport'

export const settleService = {
  candidates: (query: SettleCandidateQuery) => getSettleCandidates(query),
  createByMonth: (data: SettleByMonthDTO) => createSettleByMonth(data),
  createByOrders: (data: SettleByOrdersDTO) => createSettleByOrders(data),
  detail: (uuid: string) => getSettleOrderDetail(uuid),
  export: (params: DocumentExportInput) => exportSettleOrder(params),
  list: (query: SettleQuery) => getSettleOrderList(query),
  receive: (params: { uuid: string; data: ReceiveDTO }) => receivePayment(params.uuid, params.data),
  cancelReceive: (params: { uuid: string; receiveUuid: string; data: SettleActionReasonDTO }) =>
    cancelReceivePayment(params.uuid, params.receiveUuid, params.data),
  void: (params: { uuid: string; data: SettleActionReasonDTO }) => voidSettleOrder(params.uuid, params.data),
}
