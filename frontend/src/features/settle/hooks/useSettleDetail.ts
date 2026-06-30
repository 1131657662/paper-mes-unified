import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useSettleDetail(uuid?: string) {
  return useQuery({
    ...queries.settle.detail(uuid ?? ''),
    enabled: !!uuid,
  })
}
