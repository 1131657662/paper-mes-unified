import type { PageQuery } from './common'

export type DeliveryInventoryStockState = 1 | 2
export type DeliveryInventoryType = 1 | 2 | 3

export interface DeliveryInventoryFilter {
  customerUuid?: string
  warehouseUuid?: string
  orderUuid?: string
  keyword?: string
  stockState?: DeliveryInventoryStockState
  inventoryType?: DeliveryInventoryType
  stockAgeMinDays?: number
}

export interface DeliveryInventoryCustomerQuery extends DeliveryInventoryFilter, PageQuery {}

export interface DeliveryInventoryFinishQuery extends DeliveryInventoryFilter, PageQuery {}

export interface DeliveryInventorySummary {
  customerCount: number
  totalRollCount: number
  availableRollCount: number
  lockedRollCount: number
  productRollCount: number
  remainRollCount: number
  directRollCount: number
  totalWeight: number
  availableWeight: number
  lockedWeight: number
  plannedOutWeight: number
  stockInTimeUnknownCount: number
  asOf?: string
}

export interface DeliveryInventoryCustomer {
  customerUuid: string
  customerName: string
  totalRollCount: number
  availableRollCount: number
  lockedRollCount: number
  totalWeight: number
  availableWeight: number
  lockedWeight: number
  plannedOutWeight: number
  oldestStockInTime?: string
  stockInTimeUnknownCount: number
  warehouseCount: number
  warehouseNames?: string
  paperNames?: string
}

export interface DeliveryInventoryFinish {
  customerUuid: string
  customerName: string
  finishUuid: string
  finishRollNo: string
  orderUuid: string
  orderNo: string
  orderDate?: string
  warehouseUuid?: string
  warehouseName?: string
  warehouseLocation?: string
  paperName: string
  gramWeight?: number
  finishWidth?: number
  finishDiameter?: number
  finishCoreDiameter?: number
  remainingWeight: number
  actualWeight?: number
  stockInTime?: string
  stockAgeDays?: number
  isRemain?: number
  sourceType?: number
  stockState: DeliveryInventoryStockState
  plannedOutWeight?: number
  deliveryUuid?: string
  deliveryNo?: string
}

export interface DeliveryInventoryOrderGroup {
  orderUuid: string
  orderNo: string
  orderDate?: string
  totalRollCount: number
  totalWeight: number
  availableRollCount: number
  lockedRollCount: number
  finishes: DeliveryInventoryFinish[]
}

export interface DeliveryInventoryAvailabilityRequest {
  customerUuid: string
  warehouseUuid: string
  finishUuids: string[]
}

export interface DeliveryInventoryUnavailable {
  finishUuid: string
  reason: string
}

export interface DeliveryInventoryAvailability {
  availableFinishUuids: string[]
  unavailable: DeliveryInventoryUnavailable[]
}

export interface DeliveryInventoryUnassignedQuery {
  keyword?: string
  current: number
  size: number
}

export interface DeliveryInventoryUnassignedOrder {
  orderUuid: string
  orderNo: string
  orderDate?: string
  orderStatus: number
  customerUuid: string
  customerName: string
  unassignedRollCount: number
  unassignedWeight: number
  knownWarehouseUuid?: string
  knownWarehouseName?: string
  warehouseConflict: boolean
  activeLockCount: number
}

export interface DeliveryInventoryWarehouseRepairRequest {
  orderUuids: string[]
  warehouseUuid: string
  reason: string
}

export interface DeliveryInventoryWarehouseRepairResult {
  repairedOrderCount: number
  repairedRollCount: number
  warehouseUuid: string
  warehouseName: string
}
