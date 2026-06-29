import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

interface Options {
  enabled?: boolean
}

export function useProcessOrderDetail(uuid?: string, options: Options = {}) {
  return useQuery({
    ...queries.processOrderDetail.detail(uuid ?? ''),
    enabled: !!uuid && (options.enabled ?? true),
  })
}
