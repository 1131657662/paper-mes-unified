import request from './request'
import {
  getCustomer as getGeneratedCustomer,
  listCustomers,
} from './generated/customerReadOnly'
import type { CustomerQuery, CustomerSaveDTO } from '../types/customer'

export function pageCustomers(query: CustomerQuery) {
  return listCustomers(query)
}

export function getCustomer(uuid: string) {
  return getGeneratedCustomer(uuid)
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
