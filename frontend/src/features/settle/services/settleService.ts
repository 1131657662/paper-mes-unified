import {
  createSettleByOrders,
  getSettleCandidates,
  getSettleOrderDetail,
  getSettleOrderList,
  receivePayment,
} from '../../../api/settle'
import type {
  ReceiveDTO,
  SettleByOrdersDTO,
  SettleCandidateQuery,
  SettleQuery,
} from '../../../types/settle'

export const settleService = {
  candidates: (query: SettleCandidateQuery) => getSettleCandidates(query),
  createByOrders: (data: SettleByOrdersDTO) => createSettleByOrders(data),
  detail: (uuid: string) => getSettleOrderDetail(uuid),
  list: (query: SettleQuery) => getSettleOrderList(query),
  receive: (params: { uuid: string; data: ReceiveDTO }) => receivePayment(params.uuid, params.data),
}
