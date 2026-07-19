import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useDeliveryDetail(uuid?: string, enabled = true) {
  return useQuery({
    ...queries.delivery.detail(uuid ?? ''),
    enabled: !!uuid && enabled,
    staleTime: 5_000,
  })
}
