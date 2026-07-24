import { createQueryKeys } from '@lukemorales/query-key-factory'
import {
  getFinishCustomerSpecRevisionDetail,
  getFinishCustomerSpecRevisions,
  getFinishCustomerSpecs,
} from '../../api/customerSpecification'

export const finishCustomerSpecKeys = createQueryKeys('finishCustomerSpec', {
  current: (orderUuid: string) => ({
    queryKey: [orderUuid],
    queryFn: () => getFinishCustomerSpecs(orderUuid),
  }),
  revisions: (orderUuid: string) => ({
    queryKey: [orderUuid],
    queryFn: () => getFinishCustomerSpecRevisions(orderUuid),
  }),
  revisionDetail: (orderUuid: string, revisionUuid: string) => ({
    queryKey: [orderUuid, revisionUuid],
    queryFn: () => getFinishCustomerSpecRevisionDetail(orderUuid, revisionUuid),
  }),
})
