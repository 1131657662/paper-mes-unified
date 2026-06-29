import request from '../../../api/request'
import type { DashboardOverview } from '../../../types/dashboard'

export const dashboardService = {
  overview: () => request<DashboardOverview>({ url: '/api/dashboard/overview', method: 'get' }),
}
