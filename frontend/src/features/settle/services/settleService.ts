import {
  cancelReceivePayment,
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
  SettleByOrdersDTO,
  SettleCandidateQuery,
  SettleQuery,
} from '../../../types/settle'

export const settleService = {
  candidates: (query: SettleCandidateQuery) => getSettleCandidates(query),
  createByOrders: (data: SettleByOrdersDTO) => createSettleByOrders(data),
  detail: (uuid: string) => getSettleOrderDetail(uuid),
  export: (uuid: string) => exportSettleOrder(uuid),
  list: (query: SettleQuery) => getSettleOrderList(query),
  receive: (params: { uuid: string; data: ReceiveDTO }) => receivePayment(params.uuid, params.data),
  cancelReceive: (params: { uuid: string; receiveUuid: string; data: SettleActionReasonDTO }) =>
    cancelReceivePayment(params.uuid, params.receiveUuid, params.data),
  void: (params: { uuid: string; data: SettleActionReasonDTO }) => voidSettleOrder(params.uuid, params.data),
}
