import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useSettleDetails(uuid?: string, enabled = true) {
  return useQuery({
    ...queries.settle.details(uuid ?? ''),
    enabled: Boolean(uuid) && enabled,
  })
}
