import { useQuery } from '@tanstack/react-query'
import { warehouseKeys } from '../queries/warehouseKeys'

export function useWarehouseDetail(uuid?: string) {
  return useQuery({
    ...warehouseKeys.detail(uuid ?? ''),
    enabled: Boolean(uuid),
  })
}
