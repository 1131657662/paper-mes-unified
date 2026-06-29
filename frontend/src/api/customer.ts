import request from './request'
import type { PageResult } from '../types/common'
import type { Customer, CustomerQuery, CustomerSaveDTO } from '../types/customer'

export function pageCustomers(query: CustomerQuery) {
  return request<PageResult<Customer>>({
    url: '/api/customers',
    method: 'get',
    params: query,
  })
}

export function getCustomer(uuid: string) {
  return request<Customer>({ url: `/api/customers/${uuid}`, method: 'get' })
}

export function createCustomer(dto: CustomerSaveDTO) {
  return request<string>({ url: '/api/customers', method: 'post', data: dto })
}

export function updateCustomer(uuid: string, dto: CustomerSaveDTO) {
  return request<void>({ url: `/api/customers/${uuid}`, method: 'put', data: dto })
}

export function deleteCustomer(uuid: string) {
  return request<void>({ url: `/api/customers/${uuid}`, method: 'delete' })
}
