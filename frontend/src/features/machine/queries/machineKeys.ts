import { createQueryKeys } from '@lukemorales/query-key-factory'
import { getMachine } from '../../../api/machine'

export const machineKeys = createQueryKeys('machine', {
  detail: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => getMachine(uuid),
  }),
})
