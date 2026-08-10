import { createQueryKeys } from '@lukemorales/query-key-factory'
import { pageProcessOrders } from '../../../api/processOrder'
import type { ProcessOrderQuery } from '../../../types/processOrder'

export const processOrderListKeys = createQueryKeys('processOrderList', {
  page: (query: ProcessOrderQuery) => ({
    queryKey: [query],
    queryFn: () => pageProcessOrders(query),
  }),
})
