import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useCustomers() {
  return useQuery(queries.createOrder.customers)
}

export function useWarehouses() {
  return useQuery(queries.createOrder.warehouses)
}
