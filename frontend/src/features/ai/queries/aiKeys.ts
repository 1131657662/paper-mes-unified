import { createQueryKeys } from '@lukemorales/query-key-factory'
import { aiService } from '../services/aiService'

export const aiKeys = createQueryKeys('ai', {
  status: {
    queryKey: null,
    queryFn: aiService.status,
  },
})
