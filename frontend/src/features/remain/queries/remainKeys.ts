import { createQueryKeys } from '@lukemorales/query-key-factory'
import type { RemainInventoryQuery, RemainRegistrationQuery } from '../../../types/remain'
import { remainService } from '../services/remainService'

export const remainKeys = createQueryKeys('remain', {
  registrations: (query: RemainRegistrationQuery) => ({
    queryKey: [query],
    queryFn: () => remainService.registrations(query),
  }),
  registration: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => remainService.registration(uuid),
  }),
  inventory: (query: RemainInventoryQuery) => ({
    queryKey: [query],
    queryFn: () => remainService.inventory(query),
  }),
  adjustments: {
    queryKey: null,
    queryFn: () => remainService.adjustments(),
  },
  adjustment: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => remainService.adjustment(uuid),
  }),
  refunds: {
    queryKey: null,
    queryFn: () => remainService.refunds(),
  },
  sales: {
    queryKey: null,
    queryFn: () => remainService.sales(),
  },
  refund: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => remainService.refund(uuid),
  }),
})
