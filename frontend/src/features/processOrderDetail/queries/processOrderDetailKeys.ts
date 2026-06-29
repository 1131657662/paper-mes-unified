import { createQueryKeys } from '@lukemorales/query-key-factory'
import { getProcessOrder } from '../../../api/processOrder'

export const processOrderDetailKeys = createQueryKeys('processOrderDetail', {
  detail: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => getProcessOrder(uuid),
  }),
})
