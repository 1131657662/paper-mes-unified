import { createQueryKeys } from '@lukemorales/query-key-factory'
import {
  getDeliveryCustomerSpecRevisionDetail,
  getDeliveryCustomerSpecRevisions,
  getDeliveryCustomerSpecs,
} from '../../api/deliveryCustomerSpecification'

export const deliveryCustomerSpecKeys = createQueryKeys('deliveryCustomerSpec', {
  current: (uuid: string) => ({ queryKey: [uuid], queryFn: () => getDeliveryCustomerSpecs(uuid) }),
  revisions: (uuid: string) => ({ queryKey: [uuid], queryFn: () => getDeliveryCustomerSpecRevisions(uuid) }),
  revisionDetail: (uuid: string, revisionUuid: string) => ({
    queryKey: [uuid, revisionUuid],
    queryFn: () => getDeliveryCustomerSpecRevisionDetail(uuid, revisionUuid),
  }),
})
