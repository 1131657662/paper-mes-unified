import type { AvailableFinishPageVO } from '../../types/delivery'

interface Params {
  customerUuid?: string
  data?: AvailableFinishPageVO
  hasFilters: boolean
  isError: boolean
  scopeName: string
  warehouseUuid?: string
}

export function deliveryAvailableEmptyText(params: Params): string {
  if (!params.customerUuid) return '请先选择客户'
  if (!params.warehouseUuid) return '请先选择出库仓库'
  if (params.isError) return `可出库${params.scopeName}加载失败，请重新加载`
  if ((params.data?.total ?? 0) > 0) return `没有符合筛选条件的${params.scopeName}`
  const excluded = params.data?.excluded
  if ((excluded?.unassignedWarehouseCount ?? 0) > 0) {
    return `该客户有 ${excluded?.unassignedWarehouseCount} 卷${params.scopeName}尚未分配仓库，暂不能出库`
  }
  if ((excluded?.otherWarehouseCount ?? 0) > 0) {
    return `该客户有 ${excluded?.otherWarehouseCount} 卷${params.scopeName}位于其他仓库`
  }
  if ((excluded?.lockedCount ?? 0) > 0) {
    return `该客户有 ${excluded?.lockedCount} 卷${params.scopeName}已被其他出库单占用`
  }
  if ((excluded?.invalidWeightCount ?? 0) > 0) {
    return `该客户有 ${excluded?.invalidWeightCount} 卷${params.scopeName}缺少有效可用重量`
  }
  return params.hasFilters
    ? `没有符合筛选条件的${params.scopeName}`
    : `该客户在所选仓库暂无可出库${params.scopeName}`
}
