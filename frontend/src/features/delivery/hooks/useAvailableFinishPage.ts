import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { AvailableFinishQuery } from '../../../types/delivery'

export function useAvailableFinishPage(query: AvailableFinishQuery, enabled = true) {
  return useQuery({
    ...queries.delivery.availableFinishPage(query),
    enabled: enabled && Boolean(query.customerUuid) && Boolean(query.warehouseUuid),
  })
}
