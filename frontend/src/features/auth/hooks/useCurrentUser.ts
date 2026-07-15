import { useQuery, type UseQueryResult } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { AuthUser } from '../../../types/auth'

export function useCurrentUser(enabled: boolean): UseQueryResult<AuthUser, Error> {
  return useQuery({
    ...queries.auth.currentUser,
    enabled,
    retry: false,
  })
}
