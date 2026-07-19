import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useAvailableFinishes(customerUuid?: string, warehouseUuid?: string) {
  return useQuery({
    ...queries.delivery.availableFinishes({ customerUuid: customerUuid ?? '', warehouseUuid }),
    enabled: !!customerUuid,
    staleTime: 5_000,
  })
}
