import { useQuery, type UseQueryResult } from '@tanstack/react-query'
import { authKeys } from '../queries/authKeys'
import type { AuthUser } from '../../../types/auth'

export function useCurrentUser(enabled: boolean): UseQueryResult<AuthUser, Error> {
  return useQuery({
    ...authKeys.currentUser,
    enabled,
    refetchOnWindowFocus: 'always',
    retry: false,
  })
}
