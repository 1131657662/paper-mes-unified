import { createQueryKeys } from '@lukemorales/query-key-factory'
import { getCustomer } from '../../../api/customer'

export const customerKeys = createQueryKeys('customer', {
  detail: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => getCustomer(uuid),
  }),
})
