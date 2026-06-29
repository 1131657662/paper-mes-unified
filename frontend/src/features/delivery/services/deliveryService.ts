import {
  confirmDeliveryOrder,
  createDeliveryOrder,
  getAvailableFinishes,
  getDeliveryOrderDetail,
  getDeliveryOrderList,
} from '../../../api/delivery'
import type {
  DeliveryConfirmDTO,
  DeliveryCreateDTO,
  DeliveryQuery,
} from '../../../types/delivery'

export const deliveryService = {
  availableFinishes: (customerUuid: string) => getAvailableFinishes(customerUuid),
  create: (data: DeliveryCreateDTO) => createDeliveryOrder(data),
  confirm: (params: { uuid: string; data?: DeliveryConfirmDTO }) =>
    confirmDeliveryOrder(params.uuid, params.data),
  detail: (uuid: string) => getDeliveryOrderDetail(uuid),
  list: (query: DeliveryQuery) => getDeliveryOrderList(query),
}
