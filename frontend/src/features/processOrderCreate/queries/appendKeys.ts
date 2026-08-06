import { createQueryKeys } from '@lukemorales/query-key-factory'
import { getProcessOrderAppendSession } from '../../../api/processOrder'

export const appendKeys = createQueryKeys('processOrderAppend', {
  session: (orderUuid: string, sessionUuid: string) => ({
    queryKey: [orderUuid, sessionUuid],
    queryFn: () => getProcessOrderAppendSession(orderUuid, sessionUuid),
  }),
})
