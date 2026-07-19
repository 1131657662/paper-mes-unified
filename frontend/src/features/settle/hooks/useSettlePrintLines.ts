import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useSettlePrintLines(uuid?: string, enabled = true) {
  return useQuery({
    ...queries.settle.printLines(uuid ?? ''),
    enabled: Boolean(uuid) && enabled,
  })
}
