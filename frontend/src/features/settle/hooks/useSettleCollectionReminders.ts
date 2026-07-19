import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useSettleCollectionReminders(uuid?: string, enabled = true) {
  return useQuery({
    ...queries.settle.collectionReminders(uuid ?? ''),
    enabled: Boolean(uuid) && enabled,
  })
}
