import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { PrintViewVersion } from '../../../types/processOrder'

export function useProcessOrderPrintView(
  uuid: string | undefined,
  version: PrintViewVersion,
  enabled: boolean,
) {
  return useQuery({
    ...queries.processOrderDetail.printView(uuid ?? '', version),
    enabled: Boolean(uuid && enabled),
  })
}
