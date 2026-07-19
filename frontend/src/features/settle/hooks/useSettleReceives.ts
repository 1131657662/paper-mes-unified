import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useSettleReceives(uuid?: string, enabled = true) {
  return useQuery({
    ...queries.settle.receives(uuid ?? ''),
    enabled: Boolean(uuid) && enabled,
  })
}
