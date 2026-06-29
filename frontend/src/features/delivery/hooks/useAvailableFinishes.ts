import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useAvailableFinishes(customerUuid?: string) {
  return useQuery({
    ...queries.delivery.availableFinishes(customerUuid ?? ''),
    enabled: !!customerUuid,
  })
}
