import type { QueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'

export async function invalidateCreateOrderDraft(queryClient: QueryClient, orderUuid: string): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: queries.createOrder.draft(orderUuid).queryKey }),
    queryClient.invalidateQueries({ queryKey: queries.createOrder.drafts.queryKey }),
  ])
}
