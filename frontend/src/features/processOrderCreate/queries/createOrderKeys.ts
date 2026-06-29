import { createQueryKeys } from '@lukemorales/query-key-factory'
import { createOrderService } from '../services/createOrderService'

export const createOrderKeys = createQueryKeys('createOrder', {
  customers: {
    queryKey: null,
    queryFn: () => createOrderService.customers(),
  },
  warehouses: {
    queryKey: null,
    queryFn: () => createOrderService.warehouses(),
  },
  drafts: {
    queryKey: null,
    queryFn: () => createOrderService.drafts(),
  },
  draft: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => createOrderService.draft(uuid),
  }),
})
