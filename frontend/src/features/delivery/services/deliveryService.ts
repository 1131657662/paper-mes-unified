import {
  appendDeliveryDetails,
  cancelPendingDeliveryOrder,
  confirmDeliveryOrder,
  confirmDeliveryOrders,
  createDeliveryOrder,
  exportDeliveryOrder,
  getAvailableFinishes,
  getDeliveryOrderDetail,
  getDeliveryOrderList,
  getDeliveryOrderSummary,
  removeDeliveryDetail,
  rollbackDeliveryOrder,
} from '../../../api/delivery'
import type {
  DeliveryAppendItemsDTO,
  DeliveryBatchConfirmDTO,
  DeliveryCancelDTO,
  DeliveryConfirmDTO,
  DeliveryCreateDTO,
  DeliveryQuery,
  DeliveryRollbackDTO,
} from '../../../types/delivery'
import type { DocumentExportInput } from '../../../utils/documentExport'

export const deliveryService = {
  appendDetails: (params: { uuid: string; data: DeliveryAppendItemsDTO }) =>
    appendDeliveryDetails(params.uuid, params.data),
  availableFinishes: (customerUuid: string) => getAvailableFinishes(customerUuid),
  cancelPending: (params: { uuid: string; data: DeliveryCancelDTO }) =>
    cancelPendingDeliveryOrder(params.uuid, params.data),
  create: (data: DeliveryCreateDTO) => createDeliveryOrder(data),
  confirm: (params: { uuid: string; data?: DeliveryConfirmDTO }) =>
    confirmDeliveryOrder(params.uuid, params.data),
  confirmBatch: (data: DeliveryBatchConfirmDTO) => confirmDeliveryOrders(data),
  detail: (uuid: string) => getDeliveryOrderDetail(uuid),
  export: (params: DocumentExportInput) => exportDeliveryOrder(params),
  list: (query: DeliveryQuery) => getDeliveryOrderList(query),
  summary: (query: DeliveryQuery) => getDeliveryOrderSummary(query),
  removeDetail: (params: { uuid: string; detailUuid: string }) =>
    removeDeliveryDetail(params.uuid, params.detailUuid),
  rollback: (params: { uuid: string; data: DeliveryRollbackDTO }) =>
    rollbackDeliveryOrder(params.uuid, params.data),
}
