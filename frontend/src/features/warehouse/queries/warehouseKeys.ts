import { createQueryKeys } from '@lukemorales/query-key-factory'
import { getWarehouse } from '../../../api/warehouse'

export const warehouseKeys = createQueryKeys('warehouse', {
  detail: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => getWarehouse(uuid),
  }),
})
