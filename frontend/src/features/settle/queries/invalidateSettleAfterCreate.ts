import type { QueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'

export async function invalidateSettleAfterCreate(queryClient: QueryClient): Promise<void> {
  const options = { refetchType: 'none' as const }
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: queries.settle.candidates._def, ...options }),
    queryClient.invalidateQueries({ queryKey: queries.settle.list._def, ...options }),
    queryClient.invalidateQueries({ queryKey: queries.settle.summary._def, ...options }),
  ])
}
