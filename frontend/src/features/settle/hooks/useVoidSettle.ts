import { useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import { settleService } from '../services/settleService'
import {
  invalidateProcessOrderBusinessDependents,
  invalidateProcessOrderLocalReadModels,
} from '../../processOrderDetail/hooks/invalidateProcessOrderReadModels'

export function useVoidSettle() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: settleService.void,
    onSuccess: (orderUuids) => invalidateVoidedSettle(queryClient, orderUuids),
  })
}

export async function invalidateVoidedSettle(
  queryClient: QueryClient,
  orderUuids: string[],
): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: queries.settle._def }),
    ...orderUuids.map((uuid) => invalidateProcessOrderLocalReadModels(queryClient, uuid)),
    invalidateProcessOrderBusinessDependents(queryClient),
  ])
}
