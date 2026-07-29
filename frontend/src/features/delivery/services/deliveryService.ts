import {
  appendDeliveryDetails,
  cancelPendingDeliveryOrder,
  confirmDeliveryOrder,
  confirmDeliveryOrders,
  createDeliveryOrder,
  getAvailableFinishPage,
  getDeliveryOrderDetail,
  getDeliveryOrderList,
  getDeliveryOrderSummary,
  removeDeliveryDetail,
  rollbackDeliveryOrder,
  updatePendingDeliveryOrder,
} from '../../../api/delivery'
import type {
  DeliveryAppendItemsDTO,
  AvailableFinishQuery,
  DeliveryBatchConfirmDTO,
  DeliveryCancelDTO,
  DeliveryConfirmDTO,
  DeliveryCreateDTO,
  DeliveryQuery,
  DeliveryRollbackDTO,
} from '../../../types/delivery'
import type { DeliveryPendingUpdateDTO } from '../../../types/deliveryPendingUpdate'

export const deliveryService = {
  appendDetails: (params: { uuid: string; data: DeliveryAppendItemsDTO }) =>
    appendDeliveryDetails(params.uuid, params.data),
  availableFinishPage: (query: AvailableFinishQuery) => getAvailableFinishPage(query),
  cancelPending: (params: { uuid: string; data: DeliveryCancelDTO }) =>
    cancelPendingDeliveryOrder(params.uuid, params.data),
  create: (data: DeliveryCreateDTO) => createDeliveryOrder(data),
  confirm: (params: { uuid: string; data?: DeliveryConfirmDTO }) =>
    confirmDeliveryOrder(params.uuid, params.data),
  confirmBatch: (data: DeliveryBatchConfirmDTO) => confirmDeliveryOrders(data),
  detail: (uuid: string) => getDeliveryOrderDetail(uuid),
  list: (query: DeliveryQuery) => getDeliveryOrderList(query),
  summary: (query: DeliveryQuery) => getDeliveryOrderSummary(query),
  updatePending: (params: { uuid: string; data: DeliveryPendingUpdateDTO }) =>
    updatePendingDeliveryOrder(params.uuid, params.data),
  removeDetail: (params: { uuid: string; detailUuid: string }) =>
    removeDeliveryDetail(params.uuid, params.detailUuid),
  rollback: (params: { uuid: string; data: DeliveryRollbackDTO }) =>
    rollbackDeliveryOrder(params.uuid, params.data),
}
