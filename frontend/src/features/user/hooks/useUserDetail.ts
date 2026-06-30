import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useUserDetail(uuid?: string) {
  return useQuery({
    ...queries.user.detail(uuid ?? ''),
    enabled: !!uuid,
  })
}
