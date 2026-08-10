import { useQuery } from '@tanstack/react-query'
import { paperKeys } from '../queries/paperKeys'

export function usePaperDetail(uuid?: string) {
  return useQuery({
    ...paperKeys.detail(uuid ?? ''),
    enabled: Boolean(uuid),
  })
}
