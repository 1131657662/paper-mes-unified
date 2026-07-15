import { createQueryKeys } from '@lukemorales/query-key-factory'
import { notificationService } from '../services/notificationService'

export const notificationKeys = createQueryKeys('notifications', {
  summary: {
    queryKey: null,
    queryFn: notificationService.summary,
  },
})
