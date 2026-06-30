import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useRuntimeConfigs(keys: string[]) {
  return useQuery({
    ...queries.systemConfig.runtimeConfigs(keys),
    enabled: keys.length > 0,
    staleTime: 5 * 60 * 1000,
  })
}
