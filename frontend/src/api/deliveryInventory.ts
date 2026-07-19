import request from './request'
import type { PageResult } from '../types/common'
import type {
  DeliveryInventoryAvailability,
  DeliveryInventoryAvailabilityRequest,
  DeliveryInventoryCustomer,
  DeliveryInventoryCustomerQuery,
  DeliveryInventoryFilter,
  DeliveryInventoryFinish,
  DeliveryInventoryFinishQuery,
  DeliveryInventoryOrderGroup,
  DeliveryInventorySummary,
  DeliveryInventoryUnassignedOrder,
  DeliveryInventoryUnassignedQuery,
  DeliveryInventoryWarehouseRepairRequest,
  DeliveryInventoryWarehouseRepairResult,
} from '../types/deliveryInventory'

const INVENTORY_URL = '/api/delivery-orders/inventory'

export function getDeliveryInventoryCustomers(query: DeliveryInventoryCustomerQuery) {
  return request<PageResult<DeliveryInventoryCustomer>>({
    url: `${INVENTORY_URL}/customers`, method: 'get', params: query,
  })
}

export function getDeliveryInventorySummary(filter: DeliveryInventoryFilter) {
  return request<DeliveryInventorySummary>({
    url: `${INVENTORY_URL}/summary`, method: 'get', params: filter,
  })
}

export function getDeliveryInventoryFinishes(query: DeliveryInventoryFinishQuery) {
  return request<PageResult<DeliveryInventoryFinish>>({
    url: `${INVENTORY_URL}/finishes`, method: 'get', params: query,
  })
}

export function getDeliveryInventoryOrderGroups(query: DeliveryInventoryFinishQuery) {
  return request<PageResult<DeliveryInventoryOrderGroup>>({
    url: `${INVENTORY_URL}/order-groups`, method: 'get', params: query,
  })
}

export function validateDeliveryInventory(data: DeliveryInventoryAvailabilityRequest) {
  return request<DeliveryInventoryAvailability>({
    url: `${INVENTORY_URL}/validate-availability`, method: 'post', data,
  })
}

export function getDeliveryInventoryUnassigned(query: DeliveryInventoryUnassignedQuery) {
  return request<PageResult<DeliveryInventoryUnassignedOrder>>({
    url: `${INVENTORY_URL}/unassigned`, method: 'get', params: query,
  })
}

export function assignDeliveryInventoryWarehouse(data: DeliveryInventoryWarehouseRepairRequest) {
  return request<DeliveryInventoryWarehouseRepairResult>({
    url: `${INVENTORY_URL}/assign-warehouse`, method: 'post', data,
  })
}
