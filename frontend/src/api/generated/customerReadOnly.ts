import { orvalRequest } from '../orvalRequest'
export type CustomerProcessPriceVOBillingBasis =
  (typeof CustomerProcessPriceVOBillingBasis)[keyof typeof CustomerProcessPriceVOBillingBasis]

export const CustomerProcessPriceVOBillingBasis = {
  PIECE: 'PIECE',
  TON: 'TON',
  FIXED: 'FIXED',
} as const

export interface CustomerProcessPriceVO {
  billingBasis: CustomerProcessPriceVOBillingBasis
  billingUnitName?: string
  catalogUuid: string
  defaultOption: boolean
  price: number
  processCode?: string
  processName?: string
  stepType?: number
}

export type CustomerVOSettleType =
  (typeof CustomerVOSettleType)[keyof typeof CustomerVOSettleType]

export const CustomerVOSettleType = {
  NUMBER_1: 1,
  NUMBER_2: 2,
} as const

export interface CustomerVO {
  bankAccount?: string
  contact?: string
  createTime?: string
  customerCode?: string
  customerLevel?: number
  customerName: string
  defaultInvoice?: number
  deliveryAddress?: string
  exportTemplate?: string
  invoiceAddress?: string
  phone?: string
  priceIncludeTax?: number
  processPrices?: CustomerProcessPriceVO[]
  remark?: string
  rewindPrice?: number
  sawPrice?: number
  settleDay?: number
  settleType?: CustomerVOSettleType
  taxNo?: string
  taxRate?: number
  updateTime?: string
  uuid: string
  version?: number
}

export interface PageResultCustomerVO {
  current: number
  records: CustomerVO[]
  size: number
  total: number
}

export type ListCustomersParams = {
  keyword?: string
  current?: number
  size?: number
}

type SecondParameter<T extends (...args: never) => unknown> = Parameters<T>[1]

/**
 * @summary List customers
 */
export const listCustomers = (
  params?: ListCustomersParams,
  options?: SecondParameter<typeof orvalRequest<PageResultCustomerVO>>,
) => {
  return orvalRequest<PageResultCustomerVO>(
    { url: `/api/customers`, method: 'GET', params },
    options,
  )
}

/**
 * @summary Get a customer profile
 */
export const getCustomer = (
  uuid: string,
  options?: SecondParameter<typeof orvalRequest<CustomerVO>>,
) => {
  return orvalRequest<CustomerVO>(
    { url: `/api/customers/${uuid}`, method: 'GET' },
    options,
  )
}

export type ListCustomersResult = NonNullable<
  Awaited<ReturnType<typeof listCustomers>>
>
export type GetCustomerResult = NonNullable<
  Awaited<ReturnType<typeof getCustomer>>
>
