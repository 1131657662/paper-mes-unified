import request from './request'
import type { PageResult } from '../types/common'
import type { Warehouse, WarehouseQuery, WarehouseSaveDTO } from '../types/warehouse'

export function pageWarehouses(query: WarehouseQuery) {
  return request<PageResult<Warehouse>>({
    url: '/api/warehouses',
    method: 'get',
    params: query,
  })
}

export function getWarehouse(uuid: string) {
  return request<Warehouse>({ url: `/api/warehouses/${uuid}`, method: 'get' })
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
