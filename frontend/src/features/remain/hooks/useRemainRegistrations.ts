import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { RemainRegistrationQuery } from '../../../types/remain'

export function useRemainRegistrations(query: RemainRegistrationQuery) {
  return useQuery({ ...queries.remain.registrations(query), staleTime: 15_000 })
}
