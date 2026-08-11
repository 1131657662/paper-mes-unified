import { orvalRequest } from '../orvalRequest'
export interface Warehouse {
  createBy?: string
  createTime?: string
  extNum1?: number
  extNum2?: number
  extStr1?: string
  extStr2?: string
  isDefault?: number
  isDeleted?: number
  location?: string
  remark?: string
  status?: number
  updateBy?: string
  updateTime?: string
  uuid: string
  version?: number
  warehouseCode?: string
  warehouseName: string
}

export interface PageResultWarehouse {
  current: number
  records: Warehouse[]
  size: number
  total: number
}

export type ListWarehousesParams = {
  keyword?: string
  status?: number
  current?: number
  size?: number
}

type SecondParameter<T extends (...args: never) => unknown> = Parameters<T>[1]

/**
 * @summary List warehouses
 */
export const listWarehouses = (
  params?: ListWarehousesParams,
  options?: SecondParameter<typeof orvalRequest<PageResultWarehouse>>,
) => {
  return orvalRequest<PageResultWarehouse>(
    { url: `/api/warehouses`, method: 'GET', params },
    options,
  )
}

/**
 * @summary Get a warehouse profile
 */
export const getWarehouse = (
  uuid: string,
  options?: SecondParameter<typeof orvalRequest<Warehouse>>,
) => {
  return orvalRequest<Warehouse>(
    { url: `/api/warehouses/${uuid}`, method: 'GET' },
    options,
  )
}

export type ListWarehousesResult = NonNullable<
  Awaited<ReturnType<typeof listWarehouses>>
>
export type GetWarehouseResult = NonNullable<
  Awaited<ReturnType<typeof getWarehouse>>
>
