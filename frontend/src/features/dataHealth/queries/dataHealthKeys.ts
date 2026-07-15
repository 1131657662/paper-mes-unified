import { createQueryKeys } from '@lukemorales/query-key-factory'
import { dataHealthService } from '../services/dataHealthService'

export const dataHealthKeys = createQueryKeys('dataHealth', {
  summary: {
    queryKey: null,
    queryFn: dataHealthService.inspect,
  },
})
