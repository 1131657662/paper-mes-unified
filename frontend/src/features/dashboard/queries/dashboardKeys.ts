import { createQueryKeys } from '@lukemorales/query-key-factory'
import { dashboardService } from '../services/dashboardService'

export const dashboardKeys = createQueryKeys('dashboard', {
  overview: {
    queryKey: null,
    queryFn: dashboardService.overview,
  },
})
