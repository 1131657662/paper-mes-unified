import { createQueryKeys } from '@lukemorales/query-key-factory'
import { runtimeService } from '../services/runtimeService'

export const runtimeKeys = createQueryKeys('runtime', {
  version: {
    queryKey: null,
    queryFn: runtimeService.current,
  },
})
