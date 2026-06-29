import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useGetDraft(uuid?: string) {
  return useQuery({
    ...queries.createOrder.draft(uuid ?? ''),
    enabled: Boolean(uuid),
  })
}
