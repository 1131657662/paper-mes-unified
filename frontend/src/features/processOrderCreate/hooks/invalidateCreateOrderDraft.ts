import type { QueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'

export async function invalidateCreateOrderDraft(queryClient: QueryClient, orderUuid: string): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: queries.createOrder.draft(orderUuid).queryKey }),
    queryClient.invalidateQueries({ queryKey: queries.createOrder.drafts.queryKey }),
  ])
}

export async function invalidateSubmittedProcessOrder(
  queryClient: QueryClient,
  orderUuid: string,
): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: queries.createOrder.drafts.queryKey }),
    queryClient.invalidateQueries({ queryKey: queries.processOrderDetail.detail(orderUuid).queryKey }),
    queryClient.invalidateQueries({ queryKey: queries.dashboard._def }),
    queryClient.invalidateQueries({ queryKey: queries.report._def }),
  ])
}
