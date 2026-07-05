import type { TablePaginationConfig } from 'antd/es/table'
import { formatNumber } from '../../utils/numberFormatters'

export const mesPageSizeOptions = [10, 20, 50, 100, 200, 500, 1000]

export function mesPaginationShowTotal(total: number) {
  return `共 ${formatTotal(total)} 条`
}

export function mesPaginationTotalText(total: number) {
  return `，${mesPaginationShowTotal(total)}`
}

export function mesTablePagination(
  defaultPageSize = 10,
  overrides?: TablePaginationConfig,
): TablePaginationConfig {
  return {
    defaultPageSize,
    showSizeChanger: true,
    pageSizeOptions: mesPageSizeOptions,
    showTotal: mesPaginationShowTotal,
    ...overrides,
  }
}

function formatTotal(total: number) {
  return formatNumber(total)
}
