import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useRuntimeVersion(enabled: boolean) {
  return useQuery({
    ...queries.runtime.version,
    enabled,
    staleTime: 60_000,
  })
}
