export interface DashboardOverview {
  metrics?: DashboardMetrics
  statusQueue?: DashboardStatus[]
  monthlyTrend?: DashboardTrend[]
  yearlyTrend?: DashboardTrend[]
  customerRank?: DashboardRank[]
  customerYearRank?: DashboardRank[]
  machineRank?: DashboardRank[]
  todos?: DashboardTodo[]
  recentOrders?: DashboardRecentOrder[]
}

export interface DashboardMetrics {
  monthOrderCount?: number
  monthOriginalWeight?: number
  monthFinishWeight?: number
  monthAmount?: number
  monthLossWeight?: number
  monthLossRatio?: number
  inStockFinishCount?: number
  inStockFinishWeight?: number
  receivableCount?: number
  receivableAmount?: number
}

export interface DashboardStatus {
  status?: number
  statusName?: string
  orderCount?: number
  originalWeight?: number
}

export interface DashboardTrend {
  month?: string
  orderCount?: number
  originalWeight?: number
  finishWeight?: number
  amount?: number
}

export interface DashboardRank {
  id?: string
  name?: string
  count?: number
  weight?: number
  amount?: number
}

export interface DashboardTodo {
  key?: string
  title?: string
  description?: string
  count?: number
  amount?: number
  level?: string
  targetPath?: string
}

export interface DashboardRecentOrder {
  uuid?: string
  orderNo?: string
  customerName?: string
  orderDate?: string
  priority?: number
  orderStatus?: number
  printStatus?: number
  originalWeight?: number
  finishWeight?: number
}
