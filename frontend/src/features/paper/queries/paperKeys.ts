import { createQueryKeys } from '@lukemorales/query-key-factory'
import { getPaper } from '../../../api/paper'

export const paperKeys = createQueryKeys('paper', {
  detail: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => getPaper(uuid),
  }),
})
