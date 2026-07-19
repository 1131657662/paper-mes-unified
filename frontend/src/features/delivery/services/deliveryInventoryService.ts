import {
  assignDeliveryInventoryWarehouse,
  getDeliveryInventoryCustomers,
  getDeliveryInventoryFinishes,
  getDeliveryInventoryOrderGroups,
  getDeliveryInventorySummary,
  getDeliveryInventoryUnassigned,
  validateDeliveryInventory,
} from '../../../api/deliveryInventory'
import { createDeliveryInventoryExportTask } from '../../../api/exportTask'
import type {
  DeliveryInventoryAvailabilityRequest,
  DeliveryInventoryCustomerQuery,
  DeliveryInventoryFilter,
  DeliveryInventoryFinishQuery,
  DeliveryInventoryUnassignedQuery,
  DeliveryInventoryWarehouseRepairRequest,
} from '../../../types/deliveryInventory'

export const deliveryInventoryService = {
  customers: (query: DeliveryInventoryCustomerQuery) => getDeliveryInventoryCustomers(query),
  export: (query: DeliveryInventoryFinishQuery) => createDeliveryInventoryExportTask(query),
  finishes: (query: DeliveryInventoryFinishQuery) => getDeliveryInventoryFinishes(query),
  orderGroups: (query: DeliveryInventoryFinishQuery) => getDeliveryInventoryOrderGroups(query),
  unassigned: (query: DeliveryInventoryUnassignedQuery) => getDeliveryInventoryUnassigned(query),
  summary: (filter: DeliveryInventoryFilter) => getDeliveryInventorySummary(filter),
  assignWarehouse: (data: DeliveryInventoryWarehouseRepairRequest) => assignDeliveryInventoryWarehouse(data),
  validateAvailability: (data: DeliveryInventoryAvailabilityRequest) => validateDeliveryInventory(data),
}
