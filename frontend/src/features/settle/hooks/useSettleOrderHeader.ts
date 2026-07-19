import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useSettleOrderHeader(uuid?: string) {
  return useQuery({
    ...queries.settle.detailHeader(uuid ?? ''),
    enabled: Boolean(uuid),
  })
}
