import {
  appendDeliveryDetails,
  confirmDeliveryOrder,
  createDeliveryOrder,
  exportDeliveryOrder,
  getAvailableFinishes,
  getDeliveryOrderDetail,
  getDeliveryOrderList,
  removeDeliveryDetail,
  rollbackDeliveryOrder,
} from '../../../api/delivery'
import type {
  DeliveryAppendItemsDTO,
  DeliveryConfirmDTO,
  DeliveryCreateDTO,
  DeliveryQuery,
  DeliveryRollbackDTO,
} from '../../../types/delivery'

export const deliveryService = {
  appendDetails: (params: { uuid: string; data: DeliveryAppendItemsDTO }) =>
    appendDeliveryDetails(params.uuid, params.data),
  availableFinishes: (customerUuid: string) => getAvailableFinishes(customerUuid),
  create: (data: DeliveryCreateDTO) => createDeliveryOrder(data),
  confirm: (params: { uuid: string; data?: DeliveryConfirmDTO }) =>
    confirmDeliveryOrder(params.uuid, params.data),
  detail: (uuid: string) => getDeliveryOrderDetail(uuid),
  export: (uuid: string) => exportDeliveryOrder(uuid),
  list: (query: DeliveryQuery) => getDeliveryOrderList(query),
  removeDetail: (params: { uuid: string; detailUuid: string }) =>
    removeDeliveryDetail(params.uuid, params.detailUuid),
  rollback: (params: { uuid: string; data: DeliveryRollbackDTO }) =>
    rollbackDeliveryOrder(params.uuid, params.data),
}
