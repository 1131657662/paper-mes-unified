import request from './request'
import { getWarehouse as getGeneratedWarehouse, listWarehouses } from './generated/warehouseReadOnly'
import type { WarehouseQuery, WarehouseSaveDTO } from '../types/warehouse'

export function pageWarehouses(query: WarehouseQuery) {
  return listWarehouses(query)
}

export function getWarehouse(uuid: string) {
  return getGeneratedWarehouse(uuid)
}

export function createWarehouse(dto: WarehouseSaveDTO) {
  return request<string>({ url: '/api/warehouses', method: 'post', data: dto })
}

export function updateWarehouse(uuid: string, dto: WarehouseSaveDTO) {
  return request<void>({ url: `/api/warehouses/${uuid}`, method: 'put', data: dto })
}

export function deleteWarehouse(uuid: string) {
  return request<void>({ url: `/api/warehouses/${uuid}`, method: 'delete' })
}
