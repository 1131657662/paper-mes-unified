import { createQueryKeys } from '@lukemorales/query-key-factory'
import { reportSavedViewService } from '../services/reportSavedViewService'

export const reportSavedViewKeys = createQueryKeys('reportSavedView', {
  list: { queryKey: null, queryFn: () => reportSavedViewService.list() },
})
